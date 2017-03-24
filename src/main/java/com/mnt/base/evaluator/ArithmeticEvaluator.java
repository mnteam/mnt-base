package com.mnt.base.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mnt.base.util.CommonUtil;

public class ArithmeticEvaluator extends AbstractEvaluator {
	
	private final static char ADD 		= '+';
	private final static char SUB	 	= '-';
	private final static char MUL		= '*';
	private final static char DIV		= '/';
	private final static char MOD		= '%';
	
	public static final char[] ARITHMETIC_METHODS = {ADD, SUB, MUL, DIV, MOD};
	static {
		Arrays.sort(ARITHMETIC_METHODS);
	}
	
	private List<Evaluator> evaluators;
	private List<Character> arthMethods;

	public ArithmeticEvaluator(String expression) {
		super(expression);
	}

	@Override
	public Object eval(Object[] ms) {
		Object value = evaluators.get(0).eval(ms);
		for(int i = 0; i < arthMethods.size(); i++) {
			switch(arthMethods.get(i)) {
			case ADD : value = CommonUtil.parseAsDouble(value) + CommonUtil.parseAsDouble(evaluators.get(i + 1).eval(ms));break;
			case SUB : value = CommonUtil.parseAsDouble(value) - CommonUtil.parseAsDouble(evaluators.get(i + 1).eval(ms));break;
			case MUL : value = CommonUtil.parseAsDouble(value) * CommonUtil.parseAsDouble(evaluators.get(i + 1).eval(ms));break;
			case DIV : value = CommonUtil.parseAsDouble(value) / CommonUtil.parseAsDouble(evaluators.get(i + 1).eval(ms));break;
			case MOD : value = CommonUtil.parseAsDouble(value) % CommonUtil.parseAsDouble(evaluators.get(i + 1).eval(ms));break;
			}
		}
		
		Number vn = CommonUtil.parseAsLong(value);
		if(vn.doubleValue() == ((Number)value).doubleValue()) {
			value = vn;
		}
		
		return value;
	}

	@Override
	protected void parse() {
		evaluators = new ArrayList<Evaluator>();
		arthMethods = new ArrayList<Character>();
		
		StringBuilder sb = new StringBuilder();
		
		char c;
		int index = 0;
		boolean checkChar = true;
		boolean checkNextQuot = true;
		String val;
		
		int deep = 0;
		
		while(index < expression.length()){
			c = expression.charAt(index ++);
			switch(c) {
				case BACKSLASH : {
					checkNextQuot = !checkNextQuot;
					sb.append(c);
					
					break;
				}
				case SQL_EXPRESSION_START : {
					sb.append(c);
					deep ++;
					break;
				}
				case SQL_EXPRESSION_END : {
					sb.append(c);
					deep --;
					break;
				}
				case S_QUOT : {
					if(checkNextQuot) {
						checkChar = !checkChar;
					} else {
						checkNextQuot = true;
					}
					
					sb.append(c);
					break;
				}
				
				default : {
					
					if(checkChar) {
						if(Arrays.binarySearch(ARITHMETIC_METHODS, c) > -1 && deep == 0) {
							val = sb.toString().trim();
							evaluators.add(parseEvaluator(val));
							arthMethods.add(c);
							sb.setLength(0);
							break;
						}
					}
					
					sb.append(c);
					checkNextQuot = true;
				}
			}
		}
		
		val = sb.toString().trim();
		evaluators.add(parseEvaluator(val));
		sb.setLength(0);
	}
}
