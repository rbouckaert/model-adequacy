package beast.evolution.likelihood;

import beast.evolution.tree.Tree;

public class MATreeLikelihood extends beast.evolution.likelihood.TreeLikelihood {	
	boolean updateAlignment = false;
	
	
	
	@Override
	public double calculateLogP() {
		// TODO Auto-generated method stub
		return super.calculateLogP();
	}
	
	private void resetAlignment() {
		if (beagle != null) {
			Tree tree = (Tree) treeInput.get();			
	        for (int i = 0; i < tree.getLeafNodeCount(); i++) {
	        	int taxon = dataInput.get().getTaxonIndex(tree.getNode(i).getID()); 
		        if (m_useAmbiguities.get() || m_useTipLikelihoods.get()) {
	                beagle.setPartials(beagle.beagle, i, taxon);
	            } else {
	            	beagle.setStates(beagle.beagle, i, taxon);
	            }
	        }
		} else {
	        if (m_useAmbiguities.get() || m_useTipLikelihoods.get()) {
	            setPartials(treeInput.get().getRoot(), dataInput.get().getPatternCount());
	        } else {
	            setStates(treeInput.get().getRoot(), dataInput.get().getPatternCount());
	        }
		}
	}
	
	@Override
	protected boolean requiresRecalculation() {		
		boolean isDirty = super.requiresRecalculation();
		if (dataInput.get().isDirtyCalculation()) {
			resetAlignment();
			isDirty = true;
			updateAlignment = true;
		}
		return isDirty;
	}
	
	@Override
	public void restore() {
		if (updateAlignment) {
			resetAlignment();
		}
		super.restore();
		updateAlignment = false;
	}
	
	@Override
	public void store() {
		super.store();
		updateAlignment = false;
	}
	

}
