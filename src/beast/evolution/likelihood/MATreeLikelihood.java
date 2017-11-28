package beast.evolution.likelihood;

public class MATreeLikelihood extends beast.evolution.likelihood.TreeLikelihood {	
	boolean updateAlignment = false;
	
	public void resetAlignment() {
		if (beagle != null) {
	        if (m_useAmbiguities.get() || m_useTipLikelihoods.get()) {
	        	beagle.setPartials(treeInput.get().getRoot(), dataInput.get().getPatternCount());
	        } else {
	        	beagle.setStates(treeInput.get().getRoot(), dataInput.get().getPatternCount());
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
