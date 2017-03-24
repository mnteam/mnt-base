package com.mnt.base.evaluator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mnt.base.util.CommonUtil;

/**
 * <pre>
 * expression auto parse and eval for reuse
 * 
 * 1. check the expression match
 * 
 * e.g. [M3] { k = 1 and M2.k &gt; 3 }
 * 
 * 2. retrieve the value
 * e.g. M3.k|int or k|int
 * 
 * </pre>
 * 
 * @author Peng Peng
 *
 */
public class Expression extends AbstractEvaluator {
	
	private static final String EXPRESSION_MATCH_REGEX_STR = "^(\\s*\\{?(.*)\\}?)$";
	private static final Pattern EXPRESSION_MATCH_REGEX = Pattern.compile(EXPRESSION_MATCH_REGEX_STR);

	private LogicEvaluator logicEvaluator;
	
	public Expression(String expression) {
		super(expression);
	}
	
	protected void parse() {
		
		Matcher matcher = EXPRESSION_MATCH_REGEX.matcher(expression);
		if(matcher.find()) {
			if(matcher.groupCount() > 1 && matcher.group(2) != null) {
				logicEvaluator = new LogicEvaluator(matcher.group(2).trim());
			}
			
		} else {
			throw new RuntimeException("Invalid expression source: " + expression);
		}
	}

	public boolean match(Object ... ms) {
		boolean result = logicEvaluator == null || CommonUtil.castAsBoolean(logicEvaluator.eval(ms));
		return result;
	}

	@Override
	public Object eval(Object[] ms) {
		return match(ms);
	}
}
