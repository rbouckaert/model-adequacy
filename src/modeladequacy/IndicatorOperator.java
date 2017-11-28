package modeladequacy;

import beast.core.Input;
import beast.core.Operator;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;

public class IndicatorOperator extends Operator {
	public Input<IntegerParameter> indicatorInput = new Input<>("parameter", "indicator parameter that is periodically increased", Validate.REQUIRED);

	IntegerParameter indicator;
	@Override
	public void initAndValidate() {
		indicator = indicatorInput.get();
	}

	@Override
	public double proposal() {
		if (indicator.getValue() + 1 > indicator.getUpper()) {
			indicator.setValue(0); 
		} else {
			indicator.setValue(indicator.getValue() + 1);
		}
		return Double.POSITIVE_INFINITY;
	}

}
