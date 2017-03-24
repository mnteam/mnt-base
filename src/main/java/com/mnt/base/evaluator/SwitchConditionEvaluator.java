package com.mnt.base.evaluator;


import java.util.LinkedHashMap;
import java.util.Map;

import com.mnt.base.util.CommonUtil;

/**
 * ?!(a &gt; y):c;(m &lt; n):d;default:e|output
 * 
 * @author pengpeng01
 *
 */
public class SwitchConditionEvaluator extends AbstractEvaluator {
	
	private Map<LogicEvaluator, Evaluator> rangeEvaluatorMap;
	private Evaluator defaultEvaluator; 

	public SwitchConditionEvaluator(String expression) {
		super(expression);
	}
	
	public void parse() {
		
		rangeEvaluatorMap = new LinkedHashMap<LogicEvaluator, Evaluator>();
		
		String rangeValueExpr = expression.substring(2); 
		String[] conds = rangeValueExpr.split(SEMICOLON_REGEX);
		
		String[] keyval;
		for(String cond : conds) {
			keyval = cond.split(COLON_REGEX);
			if(keyval.length == 2) {
				if(KEY_DEFAULT.equals(keyval[0])) {
					defaultEvaluator = parseEvaluator(keyval[1]);
					break;
				} else {
					rangeEvaluatorMap.put(new LogicEvaluator(keyval[0]), parseEvaluator(keyval[1]));
				}
			}
		}
	}
	
	@Override
	public Object eval(Object[] ms) {
		Object result = null;
		boolean found = false;
		for(LogicEvaluator cond : rangeEvaluatorMap.keySet())  {
			if(CommonUtil.castAsBoolean(cond.eval(ms))) {
				result = rangeEvaluatorMap.get(cond).eval(ms);
				found = true;
				break;
			}
		}
		
		if(!found && defaultEvaluator != null) {
			result = defaultEvaluator.eval(ms);
		}
		
		return format(result);
	}
}
