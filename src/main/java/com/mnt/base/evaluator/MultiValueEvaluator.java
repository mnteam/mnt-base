package com.mnt.base.evaluator;

import java.util.ArrayList;
import java.util.List;

public class MultiValueEvaluator extends AbstractEvaluator {

	private List<Evaluator> evaluators;

	public MultiValueEvaluator(String expression) {
		super(expression);
	}
	
	public void parse() {
		List<Object> paths = splitBy(expression, COMMA, true);
		evaluators = new ArrayList<Evaluator>();
		for(Object path : paths) {
			evaluators.add(parseEvaluator((String)path));
		}
	}
	
	@Override
	public Object eval(Object[] ms) {
		
		Object[] results = new Object[evaluators.size()];
		for(int i = 0; i < evaluators.size(); i++) {
			results[i] = evaluators.get(i).eval(ms);
		}
		return format(new Object[]{results});
			
	}
}
