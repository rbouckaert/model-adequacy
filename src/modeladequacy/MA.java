package modeladequacy;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Set;

import beast.app.util.Application;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.Runnable;
import beast.core.State;
import beast.core.Input.Validate;
import beast.core.Logger;
import beast.core.parameter.IntegerParameter;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.likelihood.MATreeLikelihood;
import beast.evolution.operators.UniformOperator;
import beast.util.XMLProducer;
import beast.core.MCMC;

public class MA extends Runnable {
	final public Input<File> XMLFileInput = new Input<>("xml", "XML file containing the BEAST model to simulate from. This file can be generated in BEAUti.", Validate.REQUIRED);
	final public Input<File> logDirInput = new Input<>("logDir", "directory containing log files with a posterior sample of the XML analysis (uses current working dir if not specified)");
	final public Input<Integer> burnInPercentageInput = new Input<>("burnin", "percentage of log file to disregard as burn-in", 10);
	final public Input<Integer> alignmentCountInput = new Input<>("alignments", "number of alignments to generate (must be less than number of entries in log file once burn-in is removed)", 100);

	@Override
	public void initAndValidate() {
	}

	@Override
	public void run() throws Exception {
        Log.warning("Setting up the analysis");
		AlignmentListGenerator alg = new AlignmentListGenerator(XMLFileInput.get(), logDirInput.get(), burnInPercentageInput.get(), alignmentCountInput.get());
		AlignmentList list = alg.generateAlignmentList();
		MCMC mcmc = alg.mcmc;
		GenericTreeLikelihood treeLikelihood = AlignmentListGenerator.getTreeLikelihood(mcmc);

		Alignment data = treeLikelihood.dataInput.get();
		TaxonSet taxonset = getTaxonSet(data); 
		Set<BEASTInterface> set = data.getOutputs();
		for (BEASTInterface bo : set.toArray(new BEASTInterface[]{})) {
			if (bo instanceof TaxonSet) {
				// to prevent a TaxonSet sitting in between the indicator parameter of AlignmentList (which is a StateNode)
				// and TreeLikelihood (a CalculationNode) while TaxonSet is not a CalculationNode, we pass taxa to the
				// TaxonSet and set Alignment-input to null
				TaxonSet t = (TaxonSet) bo;
				t.taxonsetInput.get().addAll(taxonset.taxonsetInput.get());
				t.alignmentInput.set(null);
			} else {
				for (Input<?> input : bo.listInputs()) {
					if (input.get() != null && input.get().equals(data)) {
						input.setValue(list, bo);
					}
				}
			}
		}
		treeLikelihood.dataInput.setValue(list, treeLikelihood);
		
		
		// add alignment indicator
		IntegerParameter indicator = new IntegerParameter();
		indicator.setID("alignmentIndicator");
		indicator.initByName("value", 0, "upper", alignmentCountInput.get() - 1, "lower", 0);
		
		// add indicator to state
		State state = mcmc.startStateInput.get();
		state.stateNodeInput.get().add(indicator);
		
		// add indicator operator
		//UniformOperator operator = new UniformOperator();
		//operator.initByName("weight", 3.0, "parameter", indicator);		
		IndicatorOperator operator = new IndicatorOperator();
		operator.initByName("weight", 0.01, "parameter", indicator);
		mcmc.operatorsInput.get().add(operator);
		
		// add indicator to tracelog
		for (Logger logger : mcmc.loggersInput.get()) {
			if (logger.getID().equals("tracelog")) {
				logger.loggersInput.get().add(indicator);
			}
		}
	
		list.indicatorInput.setValue(indicator, list);
		list.initAndValidate();
		
		
		// replace treelikelihood by MATreeLikelihood
		MATreeLikelihood newLikelihood = new MATreeLikelihood();
		newLikelihood.initByName("tree", treeLikelihood.treeInput.get(),
				"siteModel", treeLikelihood.siteModelInput.get(),
				"data", treeLikelihood.dataInput.get(),
				"branchRateModel", treeLikelihood.branchRateModelInput.get());		
		newLikelihood.setID(treeLikelihood.getID());

		set = treeLikelihood.getOutputs();
		for (BEASTInterface bo : set.toArray(new BEASTInterface[]{})) {
			for (Input<?> input : bo.listInputs()) {
				if (input.get() != null) {
					if (input.get().equals(treeLikelihood)) {
						input.setValue(newLikelihood, bo);
					} else if (input.get() instanceof List<?>) {
						List l = (List) input.get();
						for (int i = 0; i < l.size(); i++) {
							Object o = l.get(i);
							if (o.equals(treeLikelihood)) {
								l.set(i, newLikelihood);
							}
						}
					}
				}
			}
		}

		// save current analysis to XML
		XMLProducer producer = new XMLProducer();
		String xml = producer.toXML(mcmc);
        FileWriter outfile = new FileWriter("/tmp/beast.xml");
        outfile.write(xml);
        outfile.close();
		
        Log.warning("Done set up. Start running the analysis");
        mcmc.initAndValidate();
		mcmc.run();
	}

	private TaxonSet getTaxonSet(Alignment data) {
		TaxonSet taxonSet = new TaxonSet();
		for (String taxon : data.getTaxaNames()) {
			taxonSet.taxonsetInput.get().add(new Taxon(taxon));
		}
		taxonSet.setID("newTaxonSet");
		return taxonSet;
	}

	public static void main(String[] args) throws Exception {
		new Application(new MA(), "Model Adequacy", args);
	}

}
