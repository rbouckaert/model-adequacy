package modeladequacy;

import java.io.File;
import java.io.FileWriter;

import beast.app.util.Application;
import beast.core.Input;
import beast.core.Runnable;
import beast.core.State;
import beast.core.Input.Validate;
import beast.core.Logger;
import beast.core.parameter.IntegerParameter;
import beast.core.util.Log;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.operators.UniformOperator;
import beast.util.XMLProducer;
import beast.core.MCMC;

public class MA extends Runnable {
	final public Input<File> XMLFileInput = new Input<>("xml", "XML file containing the BEAST model to simulate from. This file can be generated in BEAUti.", Validate.REQUIRED);
	final public Input<File> logDirInput = new Input<>("logdir", "directory containing log files with a posterior sample of the XML analysis (uses current working dir if not specified)");
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
		treeLikelihood.dataInput.setValue(list, treeLikelihood);
		
		// add alignment indicator
		IntegerParameter indicator = new IntegerParameter();
		indicator.setID("alignmentIndicator");
		indicator.initByName("value", 0, "upper", alignmentCountInput.get(), "lower", 0);
		
		// add indicator to state
		State state = mcmc.startStateInput.get();
		state.stateNodeInput.get().add(indicator);
		
		// add indicator operator
		UniformOperator operator = new UniformOperator();
		operator.initByName("weight", 3.0, "parameter", indicator);
		mcmc.operatorsInput.get().add(operator);
		
		// add indicator to tracelog
		for (Logger logger : mcmc.loggersInput.get()) {
			if (logger.getID().equals("tracelog")) {
				logger.loggersInput.get().add(indicator);
			}
		}
	
		
		XMLProducer producer = new XMLProducer();
		String xml = producer.toXML(mcmc);
        FileWriter outfile = new FileWriter("/tmp/beast.xml");
        outfile.write(xml);
        outfile.close();
		
        Log.warning("Done set up. Start running the analysis");
		mcmc.run();
	}

	public static void main(String[] args) throws Exception {
		new Application(new MA(), "Model Adequacy", args);
	}

}
