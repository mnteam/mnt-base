package com.mnt.base.evaluator;


import java.util.LinkedHashMap;
import java.util.Map;

import com.mnt.base.util.CommonUtil;

/**
 * ?value#rangeExpr
 *	rangeExpr: 
 *  x1,y1:a1;x2,y2:a2;default:a3
 * 
 * @author pengpeng01
 *
 */
public class RangeConditionEvaluator extends AbstractEvaluator {
	
	private Evaluator condValueEvalutor;
	private Map<double[], Evaluator> rangeEvaluatorMap;
	private Evaluator defaultEvaluator; 

	public RangeConditionEvaluator(String expression) {
		super(expression);
	}
	
	//?value#rangeExpr
	// rangeExpr: 
	// x1,y1:a1;x2,y2:a2;default:a3
	public void parse() {
		
		rangeEvaluatorMap = new LinkedHashMap<double[], Evaluator>();
		
		int index = expression.indexOf(STR_POUND);
		String condValueExpr = expression.substring(1, index).trim();
		String rangeValueExpr = expression.substring(index + 1); 
		
		condValueEvalutor = parseEvaluator(condValueExpr);
		
		String[] conds = rangeValueExpr.split(SEMICOLON_REGEX);
		
		String[] keyval;
		String[] minmax;
		double[] minmaxV;
		for(String cond : conds) {
			keyval = cond.split(COLON_REGEX);
			if(keyval.length == 2) {
				if(KEY_DEFAULT.equals(keyval[0])) {
					defaultEvaluator = parseEvaluator(keyval[1]);
					break;
				} else {
					minmax = keyval[0].split(COMMA_REGEX);
					if(minmax.length == 2) {
						minmaxV = new double[2];
						minmaxV[0] = CommonUtil.parseAsDouble(minmax[0].trim());
						minmaxV[1] = CommonUtil.parseAsDouble(minmax[1].trim());
						
						rangeEvaluatorMap.put(minmaxV, parseEvaluator(keyval[1]));
					} else {
						throw new RuntimeException("Invalid range condition expr: " + expression);
					}
				}
			}
		}
	}
	
	@Override
	public Object eval(Object[] ms) {
		Object value = condValueEvalutor.eval(ms);
		
		boolean found = false;
		if(value instanceof Number || value instanceof String){
			double v = CommonUtil.parseAsDouble(value);
			value = null;
			for(double[] range : rangeEvaluatorMap.keySet())  {
				if(range[0] <= v && range[1] > v) {
					value = rangeEvaluatorMap.get(range).eval(ms);
					found = true;
				}
			}
		}else{
			value = null;
		}
		
		if(!found && defaultEvaluator != null) {
			value = defaultEvaluator.eval(ms);
		}
		
		return format(value);
	}
}
 