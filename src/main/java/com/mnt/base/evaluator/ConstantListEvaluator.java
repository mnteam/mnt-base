package com.mnt.base.evaluator;

import java.util.List;

/**
 * Simple get value evalutaor
 * 
 * @author pengpeng01
 *
 */
public class ConstantListEvaluator extends AbstractEvaluator {
	
	private List<Object> constantValue;

	public ConstantListEvaluator(String expression) {
		super(expression);
	}
	
	public void parse() {
		constantValue = splitBy(expression, COMMA);
	}
	
	@Override
	public Object eval(Object[] ms) {
		return constantValue;
	}
}
