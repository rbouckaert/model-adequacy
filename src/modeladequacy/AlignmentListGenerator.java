package modeladequacy;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beast.app.seqgen.SequenceSimulator;
import beast.app.treeannotator.TreeAnnotator;
import beast.app.treeannotator.TreeAnnotator.MemoryFriendlyTreeSet;
import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Logger;
import beast.core.Logger.LOGMODE;
import beast.core.MCMC;
import beast.core.StateNode;
import beast.core.parameter.Parameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.LogAnalyser;
import beast.util.XMLParser;
import beast.util.XMLParserException;

@Description("Simulates a number of alignments from an XML analysis")
public class AlignmentListGenerator extends BEASTObject {
	final public Input<File> XMLFileInput = new Input<>("xml", "XML file containing the BEAST model to simulate from. This file can be generated in BEAUti.", Validate.REQUIRED);
	final public Input<File> logDirInput = new Input<>("logdir", "directory containing log files with a posterior sample of the XML analysis (uses current working dir if not specified)");
	final public Input<Integer> burnInPercentageInput = new Input<>("burnin", "percentage of log file to disregard as burn-in", 10);
	final public Input<Integer> alignmentCountInput = new Input<>("alignments", "number of alignments to generate (must be less than number of entries in log file once burn-in is removed)", 100);
	
	
	public AlignmentListGenerator() {
	}
	
	public AlignmentListGenerator(File xml, File logDir, int burnInPercentage, int alignmentCount) {
		initByName("xml", xml, "logdir", logDir, "burnin", burnInPercentage, "alignments", alignmentCount);
	}
	
	
	File xml;
	File logDir;
	int burnInPercentage;
	int alignemntCount;
	String traceLogFile;
	String treeFile;
	MCMC mcmc;
	
	@Override
	public void initAndValidate() {
		xml = XMLFileInput.get();
		logDir = logDirInput.get();
		burnInPercentage = burnInPercentageInput.get();
		alignemntCount = alignmentCountInput.get();
		
		
		XMLParser parser = new XMLParser();
		try {
			mcmc = (MCMC) parser.parseFile(xml);
		} catch (SAXException | IOException | ParserConfigurationException | XMLParserException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
		for (Logger logger : mcmc.loggersInput.get()) {
			if (logger.mode == LOGMODE.tree) {
				treeFile = logger.fileNameInput.get();
			} else if (logger.mode == LOGMODE.compound && logger.fileNameInput.get() != null) {
				traceLogFile = logger.fileNameInput.get();
			}
		}
	}

	
	public AlignmentList generateAlignmentList() throws IOException {
		LogAnalyser traceLog = new LogAnalyser(logDir.getAbsolutePath() + "/" + traceLogFile, burnInPercentage);
		MemoryFriendlyTreeSet treeSet = new TreeAnnotator().new MemoryFriendlyTreeSet(logDir.getAbsolutePath() + "/" + treeFile, burnInPercentage);
		treeSet.reset();
		
		GenericTreeLikelihood treeLikelihood = getTreeLikelihood(mcmc);
		Set<StateNode> stateNodes = new LinkedHashSet<>();
		getStateAncestors(treeLikelihood, stateNodes);
		AlignmentList list = new AlignmentList();
		SequenceSimulator simulator = getSimulator(treeLikelihood);
		for (int i = 0; i < alignemntCount; i++) {
			initialiseState(stateNodes, i, traceLog, treeSet);
			Alignment alignment = simulator.simulate();			
			list.alignmentsInput.get().add(alignment);
		}
		return list;
	}

	private void initialiseState(Set<StateNode> stateNodes, int i, LogAnalyser traceLog,
			MemoryFriendlyTreeSet treeSet) throws IOException {
		if (!treeSet.hasNext()) {
			throw new IllegalArgumentException("Too many trees requested fromo the tree file");
		}
		if (i >= traceLog.getTrace(0).length) {
			throw new IllegalArgumentException("Too many log entries requested fromo the trace log file");
		}

		Tree tree = scaleByRate(treeSet);
		
		
		// set up individual state node values stored in log files
		List<String> labels = traceLog.getLabels();
		for (StateNode stateNode : stateNodes) {
			if (stateNode instanceof Tree) {
				if (tree == null) {
					throw new IllegalArgumentException("Not implemented yet: there is more than 1 tree in the state");
				}
				Tree t = (Tree) stateNode;
				t.assignFrom(tree);
				tree = null; // make sure there is only one tree
			} if (stateNode instanceof Parameter) {
				Parameter param = (Parameter) stateNode;
				String label = stateNode.getID();
				if (stateNode.getDimension() == 1) {
					Double [] trace = traceLog.getTrace(label);
					if (trace == null) {
						label = label.substring(0, label.indexOf('.'));
						trace = traceLog.getTrace(label);
					}
					if (trace == null) {
						throw new IllegalArgumentException("Could not find entry for " + stateNode.getID() + " in tracelog");
					}
					param.setValue(trace[i]);					
				} else {
					for (int j = 0; j < param.getDimension(); j++) {
						Double [] trace = traceLog.getTrace(label + j);
						if (trace == null) {
							label = label.substring(0, label.indexOf('.')) + j;
							trace = traceLog.getTrace(label);
						}
						if (trace == null) {
							throw new IllegalArgumentException("Could not find entry for " + stateNode.getID() + j + " in tracelog");
						}
						param.setValue(j, trace[i]);					
					}
				}
			}
		}

		// make sure internal states are up to date
		mcmc.robustlyCalcPosterior(mcmc.posteriorInput.get());
	}

	private Tree scaleByRate(MemoryFriendlyTreeSet treeSet) throws IOException {
		Tree tree = treeSet.next();
		double [] lengths = new double[tree.getNodeCount()];
		for (Node node : tree.getNodesAsArray()) {
			Object rate = node.getMetaData("rate");
			if (rate != null) {
				Double r = (Double) rate;
				lengths[node.getNr()] = node.getLength() * r;
			} else {
				lengths[node.getNr()] = node.getLength();
			}			
		}
		setHeights(tree.getRoot(), lengths);		
		return tree;
	}

	private void setHeights(Node node, double[] lengths) {
		for (Node child : node.getChildren()) {
			child.setHeight(node.getHeight() - lengths[child.getNr()]);
			setHeights(child, lengths);
		}
	}

	private void getStateAncestors(BEASTInterface o, Set<StateNode> stateNodes) {
		if (o instanceof StateNode) {
			StateNode stateNode = (StateNode) o;
			if (mcmc.startStateInput.get().stateNodeInput.get().contains(stateNode)) {
				stateNodes.add(stateNode);
			}
		}
		for (BEASTInterface bo : o.listActiveBEASTObjects()) {
			getStateAncestors(bo, stateNodes);
		}
	}

	private SequenceSimulator getSimulator(GenericTreeLikelihood treeLikelihood) {
		SequenceSimulator simulator = new SequenceSimulator();
		simulator.initByName("tree", treeLikelihood.treeInput.get(),
				"siteModel", treeLikelihood.siteModelInput.get());		
		return simulator;
	}

	static GenericTreeLikelihood getTreeLikelihood(BEASTInterface o) {
		if (o instanceof GenericTreeLikelihood) {
			return (GenericTreeLikelihood) o;
		}
		for (BEASTInterface bo : o.listActiveBEASTObjects()) {
			GenericTreeLikelihood r = getTreeLikelihood(bo);
			if (r != null) {
				return r;
			}
		}
		return null;
	}
	
}