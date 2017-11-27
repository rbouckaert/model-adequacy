package modeladequacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.datatype.DataType;

@Description("Alignment holding a list of alignments, indexed by an indicator")
public class AlignmentList extends Alignment {
	final public Input<List<Alignment>> alignmentsInput = new Input<>("alignment", "set of alignments making up the AlignmentList", new ArrayList<>());
	final public Input<IntegerParameter> indicatorInput = new Input<>("indicator", "indicates which of the alignments of the list is the current alignment", Validate.REQUIRED);
	
	IntegerParameter indicator;
	List<Alignment> alignments;
	Alignment currentAlignment;
	int prevAlignment;
	
	@Override
	public void initAndValidate() {
		indicator = indicatorInput.get();
		alignments = alignmentsInput.get();
		prevAlignment = -1;
		currentAlignment = alignments.get(indicator.getValue());
		
		maxStateCount = currentAlignment.getMaxStateCount();
	}
	
	
	
	@Override
	protected boolean requiresRecalculation() {
		if (indicator.isDirtyCalculation()) {
			currentAlignment = alignments.get(indicator.getValue());
			return true;
		}
		return false;
	}
	
	@Override
	protected void store() {
		prevAlignment = indicator.getValue();
		super.store();
	}
	
	@Override
	protected void restore() {
		if (prevAlignment == indicator.getValue()) {
			super.restore();			
		}
		prevAlignment = indicator.getValue();
	}





	@Override
    public List<Integer> getStateCounts() {
        return currentAlignment.getStateCounts();
    }

	@Override
    public List<List<Integer>> getCounts() {
        return currentAlignment.getCounts();
    }

	@Override
    public DataType getDataType() {
        return currentAlignment.getDataType();
    }

	@Override
    public int getTaxonCount() {
		return currentAlignment.getTaxonCount();
    }

	@Override
    public int getNrTaxa() {
        return currentAlignment.getTaxonCount();
    }

	@Override
    public int getTaxonIndex(String id) {
        return currentAlignment.getTaxonIndex(id);
    }

	@Override
    public int getPatternCount() {
        return currentAlignment.getPatternCount();
    }

	@Override
    public int[] getPattern(int patternIndex_) {
        return currentAlignment.getPattern(patternIndex_);
    }

	@Override
    public int getPattern(int taxonIndex, int patternIndex_) {
        return currentAlignment.getPattern(taxonIndex, patternIndex_);
    }

	@Override
    public int getPatternWeight(int patternIndex_) {
        return currentAlignment.getPatternWeight(patternIndex_);
    }

	@Override
    public int getMaxStateCount() {
        return currentAlignment.getMaxStateCount();
    }

	@Override
    public int getPatternIndex(int site) {
        return currentAlignment.getPatternIndex(site);
    }

	@Override
    public int getSiteCount() {
        return currentAlignment.getSiteCount();
    }

	@Override
    public int[] getWeights() {
        return currentAlignment.getWeights();
    }


//	@Override
//    protected void calcPatterns() {
//        currentAlignment.calcPatterns();
//    }

	@Override
	public String toString(boolean singleLine) {
		return currentAlignment.toString(singleLine);
    }

	@Override
    public double[] getTipLikelihoods(int taxonIndex, int patternIndex_) {
		return currentAlignment.getTipLikelihoods(taxonIndex, patternIndex_);
    }

	@Override
    public boolean[] getStateSet(int state) {
		return currentAlignment.getStateSet(state);
    }

//	@Override
//    boolean isAmbiguousState(int state) {
//        return (state < 0 || state >= maxStateCount);
//    }

	@Override
    public Set<Integer> getExcludedPatternIndices() {
        return currentAlignment.getExcludedPatternIndices();
    }

	@Override
    public int getExcludedPatternCount() {
        return currentAlignment.getExcludedPatternCount();
    }

	@Override
	public double getAscertainmentCorrection(double[] patternLogProbs) {
		return currentAlignment.getAscertainmentCorrection(patternLogProbs);
    } // getAscertainmentCorrection


	@Override
	public String getSequenceAsString(String taxon) {
		return currentAlignment.getSequenceAsString(taxon);
	}
}
