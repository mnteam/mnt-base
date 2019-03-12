package com.mnt.base.evaluator;

public class ConstantEvaluator extends AbstractEvaluator {

	Object constantVal = null;
	
	public ConstantEvaluator(Object val) {
		this.constantVal = val;
	}
	
	@Override
	public Object eval(Object[] ms) {
		return constantVal;
	}

	@Override
	protected void parse() {
		constantVal = this.expression;
	}
}
