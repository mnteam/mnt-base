package com.mnt.base.evaluator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.mnt.base.util.CommonUtil;
import com.mnt.base.util.IndexableString;


/**
 * <pre>
 * providing the method to evaluate the simple expression
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
public abstract class AbstractEvaluator implements Evaluator {
	
	protected static String MAP_INDEX_PREFIX = "M";
	
	static {
		SQL_EVAL_MAP.put(SQL_EXPRESSION_EQ, SQL_EXPRESSION_EQ_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_EQ2, SQL_EXPRESSION_EQ_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_NE, SQL_EXPRESSION_NE_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_LT, SQL_EXPRESSION_LT_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_LE, SQL_EXPRESSION_LE_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_GT, SQL_EXPRESSION_GT_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_GE, SQL_EXPRESSION_GE_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_IN, SQL_EXPRESSION_IN_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_INS, SQL_EXPRESSION_INS_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_NOT_IN, SQL_EXPRESSION_NOT_IN_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_NOT_INS, SQL_EXPRESSION_NOT_INS_N);
		
		SQL_EVAL_MAP.put(SQL_EXPRESSION_AND, SQL_EXPRESSION_AND_N);
		SQL_EVAL_MAP.put(SQL_EXPRESSION_OR, SQL_EXPRESSION_OR_N);
		
		try {
			registerFormatter("int", CommonUtil.class.getDeclaredMethod("parseAsInt", Object.class));
			registerFormatter("float", CommonUtil.class.getDeclaredMethod("parseAsFloat", Object.class));
			registerFormatter("long", CommonUtil.class.getDeclaredMethod("parseAsLong", Object.class));
			registerFormatter("double", CommonUtil.class.getDeclaredMethod("parseAsDouble", Object.class));
			registerFormatter("boolean", CommonUtil.class.getDeclaredMethod("parseAsBoolean", Object.class));
			registerFormatter("string", String.class.getDeclaredMethod("valueOf", Object.class));
			registerFormatter("concat", CommonUtil.class.getDeclaredMethod("concat", Object[].class));
			
			
		} catch (Exception e) {
			throw new RuntimeException("Error when adding default function support.", e);
		}
	}
	
	protected String expression;
	private OutputFormatter outputFormatter;
	
	public AbstractEvaluator() {
		// empty
	}
	
	public Object eval(Object val) {
		return eval(new Object[]{val});
	}
	
	public static void setMapIndexPrefiex(String prefix) {
		MAP_INDEX_PREFIX = prefix;
	}
	
	public AbstractEvaluator(String valuePath) {
		this.expression = valuePath;
		parse();
	}

	public static boolean registerFormatter(String funcName, Method method) {
		return OutputFormatter.register(funcName, method);
	}
	
	public static int getMatchEnd(IndexableString source, char endChar, char startChar) {
		int index = 0;
		int checkNextEnd = 0;
		boolean checkChar = true;
		char ch;
		
		whileLabel:
		while(index < source.length()){
			ch = source.charAt(index ++);
			switch(ch) {
				case S_QUOT : {
					checkChar = !checkChar;
					break;
				}
				default : {
					
					if(checkChar) {
						if(ch == startChar && ' ' != startChar) {
							checkNextEnd ++;
						} else if(ch == endChar) {
							if(checkNextEnd == 0) {
								break whileLabel;
							} else {
								checkNextEnd --;
							}
						}
					}
				}
			}
		}

		return index;
	}
	
	@Override
	public void setOutputFormatter(OutputFormatter outputFormatter) {
		this.outputFormatter = outputFormatter;
	}
	
	public Object format(Object... objs) {
		// we just judge the first param to check if it is null
		if(outputFormatter != null && objs[0] != null) {
			return outputFormatter.format(objs);
		}
		
		return objs[0];
	}
	
	public static Object convert(String str) {
		if(str.charAt(0) == S_QUOT && str.charAt(str.length() - 1) == S_QUOT) {
			return str.substring(1, str.length() - 1);
		} else if(str.matches(NUMBER_REGEX)) {
			return CommonUtil.parseAsDouble(str);
		} else {
			throw new RuntimeException("invalid constant value: " + str);
		}
	}
	
	public static List<Object> splitBy(String source, char ch) {
		return splitBy(source, ch, false);
	}
	
	public static boolean existsInCurrentLayer(String source, char... chs) {
		boolean flag = false;
		
		char c;
		int index = 0;
		boolean checkChar = true;
		boolean checkNextQuot = true;
		
		int deep = 0;
		
		whileALl:
		while(index < source.length()){
			c = source.charAt(index ++);
			switch(c) {
				case BACKSLASH : {
					checkNextQuot = !checkNextQuot;
					
					break;
				}
				case SQL_EXPRESSION_START : {
					deep ++;
					break;
				}
				case SQL_EXPRESSION_END : {
					deep --;
					break;
				}
				case S_QUOT : {
					if(checkNextQuot) {
						checkChar = !checkChar;
					} else {
						checkNextQuot = true;
					}
					
					break;
				}
				
				default : {
					if(checkChar) {
						if(deep == 0) {
							
							for(char ch : chs) {
								if(ch == c) {
									flag = true;
									break whileALl;
								}
							}
						}
					}
					
					checkNextQuot = true;
				}
			}
		}
		
		return flag;
	}
	
	public static List<Object> splitBy(String source, char ch, boolean ignoreConvert) {
		List<Object> items = new ArrayList<Object>();
		
		StringBuilder sb = new StringBuilder();
		
		char c;
		int index = 0;
		boolean checkChar = true;
		boolean checkNextQuot = true;
		String val;
		
		int deep = 0;
		
		while(index < source.length()){
			c = source.charAt(index ++);
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
						if(c == ch && deep == 0) {
							val = sb.toString().trim();
							items.add(ignoreConvert ? val : convert(val));
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
		items.add(ignoreConvert ? val : convert(val));
		sb.setLength(0);
		
		return items;
	}
	
	public static Evaluator parseEvaluator(String expression) {
		
		while(expression.charAt(0) == SQL_EXPRESSION_START && expression.charAt(expression.length() - 1) == SQL_EXPRESSION_END) {
			expression = expression.substring(1, expression.length() - 1);
		}
		
		Evaluator evaluator;
		OutputFormatter outputFormatter = null;
		
		String valuePath;
		int pipeIndex = expression.lastIndexOf(PIPE);
		int exprEndIndex = expression.lastIndexOf(SQL_EXPRESSION_END);
		
		if(pipeIndex == -1 || pipeIndex < exprEndIndex) { // no output func
			valuePath = expression;
		} else {
			valuePath = expression.substring(0, pipeIndex);
			outputFormatter = OutputFormatter.getByName(expression.substring(pipeIndex + 1));
		}
		
		int questionMarkIndex = valuePath.indexOf(QUESTION_MARK);
		
		// condition evaluator
		if(questionMarkIndex > 0) {
			evaluator = new ConditionEvaluator(valuePath, questionMarkIndex);
		} else if(questionMarkIndex == -1) {
			
			if(existsInCurrentLayer(valuePath, COMMA)) {
				evaluator = new MultiValueEvaluator(valuePath);
			} else if(existsInCurrentLayer(valuePath, ArithmeticEvaluator.ARITHMETIC_METHODS)) {
				evaluator = new ArithmeticEvaluator(valuePath);
			} else {
				evaluator = new GetValueEvaluator(valuePath);
			}
		} else if(questionMarkIndex == 0 && valuePath.indexOf(STR_EXCLAMATORY_MARK) == 1){
			evaluator = new SwitchConditionEvaluator(valuePath);
		}else if(questionMarkIndex == 0 && valuePath.indexOf(STR_POUND) != 0){
			evaluator = new RangeConditionEvaluator(valuePath);
		} else {
			throw new RuntimeException("invalid value path: " + valuePath);
		}
		
		if(outputFormatter != null) {
			evaluator.setOutputFormatter(outputFormatter);
		}
		return evaluator;
	}
	
	protected abstract void parse();

	@Override
	public String toString() {
		return "AbstractEvaluator [expression=" + expression + "]";
	}
}
