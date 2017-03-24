package com.mnt.base.evaluator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mnt.base.util.CommonUtil;
import com.mnt.base.util.IndexableString;

/**
 * <pre>
 * providing the method to evaluate the simple expression
 * 
 * 1. check the expression match
 * 
 * e.g. [M3] { k = 1 and M2.k > 3 }
 * 
 * 2. retrieve the value
 * e.g. M3.k|int or k|int
 * 
 * </pre>
 * 
 * @author Peng Peng
 *
 */
public class LogicEvaluator extends AbstractEvaluator {
	
	private List<Evaluator> subEvaluators;
	private List<Integer> logicOps;
	private boolean notFlag;
	
	public LogicEvaluator(String expression) {
		super(expression);
	}
	
	public LogicEvaluator(String expression, boolean notFlag) {
		super(expression);
		this.notFlag = notFlag;
	}
	
	public void parse() {
		subEvaluators = new ArrayList<Evaluator>();
		logicOps = new ArrayList<Integer>();
		
		IndexableString exprSource = new IndexableString(expression);
		exprSource.trimStart();
		
		if(exprSource.length() > 0) {
			
			String token;
			while(true) {
				if(exprSource.charEquals(0, SQL_EXPRESSION_START) || exprSource.charEquals(0, SQL_NOT_FLAG_CHAR)) {
					
					boolean notFlag = exprSource.charEquals(0, SQL_NOT_FLAG_CHAR);
					if(notFlag) {
						exprSource.moveIndex();
						exprSource.trimStart();
						if(!exprSource.charEquals(0, SQL_EXPRESSION_START)) {
							throw new RuntimeException("invalid logic eval expression, should be ! (expr)");
						}
					}
					
					exprSource.moveIndex();
					int end = getMatchEnd(exprSource, SQL_EXPRESSION_END, SQL_EXPRESSION_START);
					
					subEvaluators.add(new LogicEvaluator(exprSource.valueBy(0, end - 1).trim()));
					
					
					exprSource.moveIndex(end);
					exprSource.trimStart();
					
					if(exprSource.length() > 0) { // if true, check the next condition
						
						end = exprSource.indexOf(SQL_EXPRESSION_SPACE);
						
						if(end > 0) {
							token = exprSource.valueBy(end).trim().toUpperCase();
							Integer logicType = SQL_EVAL_MAP.get(token);
							if(logicType == null || (logicType / POW_N) != SQL_EXPRESSION_AND_OR) {
								throw new RuntimeException("invalid logic expression: " + expression + ", expect the logic type and|or");
							}

							logicOps.add(logicType);
							exprSource.moveIndex(end);
							exprSource.trimStart();
						}
					} else {
						return;
					}
				} else { // leftParam logicEval rightParam || ! ()
					
					boolean inFlag = false;
					
					int end = exprSource.indexOf(SQL_EXPRESSION_SPACE);
					if(end > 0) {
						subEvaluators.add(AbstractEvaluator.parseEvaluator((exprSource.valueBy(end).trim())));
						exprSource.moveIndex(end);
						exprSource.trimStart();
					}
					
					token = null;
					end = exprSource.indexOf(SQL_EXPRESSION_SPACE);
					if(end > 0) {
						token = exprSource.valueBy(end).toUpperCase();
						exprSource.moveIndex(end);
						exprSource.trimStart();
						
						if(SQL_EXPRESSION_NOT.equals(token)) { // match 'NOT IN'
							
							String in = exprSource.valueBy(3).trim().toUpperCase();
							if(SQL_EXPRESSION_IN.equals(in)) {
								token = SQL_EXPRESSION_NOT_IN;
								exprSource.moveIndex(2);
								exprSource.trimStart();
							} else if(SQL_EXPRESSION_INS.equals(in)) {
								token = SQL_EXPRESSION_NOT_INS;
								exprSource.moveIndex(3);
								exprSource.trimStart();
							} else {
								throw new RuntimeException("support not in eval logic only, get not " + in);
							}
							
							inFlag = true;
						} else if(SQL_EXPRESSION_IN.equals(token) || SQL_EXPRESSION_INS.equals(token)){
							inFlag = true;
						}
					}
					
					Integer logicTypeIndex = SQL_EVAL_MAP.get(token);
					if(logicTypeIndex != null) {
						logicOps.add(logicTypeIndex);
					} else {
						throw new RuntimeException("invalid logic eval function: " + token);
					}
					
					end = exprSource.indexOf(inFlag ? SQL_EXPRESSION_ENDSTR : SQL_EXPRESSION_SPACE);
					if(end > 0) {
						if(inFlag) {
							end ++; // 
						}
						token = exprSource.valueBy(end);
						exprSource.moveIndex(end);
						exprSource.trimStart();
					} else { // match the end
						end = exprSource.length();
						token = exprSource.valueBy(end);
						exprSource.moveIndex(end);
					}
					
					if(inFlag) {
						subEvaluators.add(new ConstantListEvaluator(token.substring(1, token.length() - 1)));
					} else {
						subEvaluators.add(AbstractEvaluator.parseEvaluator((token)));
					}
					
					if(exprSource.length() > 0) { // if true, check the next condition
						
						end = exprSource.indexOf(SQL_EXPRESSION_SPACE);
						
						if(end > 0) {
							
							token = exprSource.valueBy(end).trim().toUpperCase();
							Integer logicType = SQL_EVAL_MAP.get(token);
							if(logicType == null || (logicType / POW_N) != SQL_EXPRESSION_AND_OR) {
								throw new RuntimeException("invalid logic expression: " + expression + ", expect the logic type and|or");
							}

							logicOps.add(logicType);
							exprSource.moveIndex(end);
							exprSource.trimStart();
						}
					} else {
						break;
					}
				}
			}
		}
	}

	@Override
	public Object eval(Object[] ms) {
		List<Object> valueList = new ArrayList<Object>();
		int firstLayerSize = 0 ;
		
		for(Evaluator evaluator : subEvaluators) {
			valueList.add(evaluator.eval(ms));
		}
		
		int evalCode;
		int indexDelta = 0;
		int index;
		boolean result = false;
		// calculate the basic logic 
		for(int i = 0; i < this.logicOps.size(); i++) {
			evalCode = logicOps.get(i);
			if(evalCode / POW_N == SQL_EXPRESSION_AND_OR) {
				
				result = CommonUtil.castAsBoolean(valueList.get(0));
				
				continue;
			}
			
			firstLayerSize ++;
			index = i - indexDelta ++;
			
			result = eval(valueList.get(index), valueList.remove(index + 1), evalCode);
			
			if((result && evalCode == SQL_EXPRESSION_OR_N) || ((!result) && evalCode == SQL_EXPRESSION_AND_N)) {
				// skip the rest calculate
				break;
			}
			
			valueList.set(index, result);
		}
		
		if(logicOps.size() > firstLayerSize) {
			boolean left, right;
			// calculate the combine logic 
			for(int i = 0; i < this.logicOps.size(); i++) {
				evalCode = logicOps.get(i);
				if(evalCode / POW_N == SQL_EXPRESSION_AND_OR) {
					
					left = CommonUtil.parseAsBoolean(valueList.get(0));
					
					if((left && evalCode == SQL_EXPRESSION_OR_N) || ((!left) && evalCode == SQL_EXPRESSION_AND_N)) {
						break;
					}
					
					right = CommonUtil.parseAsBoolean(valueList.remove(1));
					result = (evalCode == SQL_EXPRESSION_AND_N) ? (left && right) : (left || right);
					
					valueList.set(0, result);
				}
			}
		}
	
		return notFlag ? !CommonUtil.castAsBoolean(valueList.get(0)) : CommonUtil.castAsBoolean(valueList.get(0));
	}
	
	public boolean eval(Object leftParam, Object rightParam, int evalCode) {
		boolean result = false;
		switch(evalCode) {
			case SQL_EXPRESSION_EQ_N : {
				if(leftParam instanceof Number || rightParam instanceof Number) {
					result = CommonUtil.parseAsDouble(leftParam) == CommonUtil.parseAsDouble(rightParam);
				} else {
					result = CommonUtil.isEmpty(leftParam) ? CommonUtil.isEmpty(rightParam) : leftParam.equals(rightParam);
				}
				
				break;
			}
			
			case SQL_EXPRESSION_NE_N : {
				if(leftParam instanceof Number || rightParam instanceof Number) {
					result = CommonUtil.parseAsDouble(leftParam) != CommonUtil.parseAsDouble(rightParam);
				} else {
					result = CommonUtil.isEmpty(leftParam) ? !CommonUtil.isEmpty(rightParam) : !leftParam.equals(rightParam);
				}
				
				break;
			}
			
			case SQL_EXPRESSION_LT_N : {
				if(leftParam instanceof Number || rightParam instanceof Number) {
					result = CommonUtil.parseAsDouble(leftParam) < CommonUtil.parseAsDouble(rightParam);
				} else if(leftParam instanceof Date) {
					
					if(rightParam instanceof Date) {
						result = ((Date)leftParam).getTime() < ((Date)rightParam).getTime();
					} else {
						result = ((Date)leftParam).getTime() < CommonUtil.parseAsLong(rightParam);
					}
				} else if(rightParam instanceof Date) {
					result = CommonUtil.parseAsLong(leftParam) < ((Date)leftParam).getTime();
				}
				
				break;
			}
			
			case SQL_EXPRESSION_LE_N : {
				if(leftParam instanceof Number || rightParam instanceof Number) {
					result = CommonUtil.parseAsDouble(leftParam) <= CommonUtil.parseAsDouble(rightParam);
				} else if(leftParam instanceof Date) {
					
					if(rightParam instanceof Date) {
						result = ((Date)leftParam).getTime() <= ((Date)rightParam).getTime();
					} else {
						result = ((Date)leftParam).getTime() <= CommonUtil.parseAsLong(rightParam);
					}
				} else if(rightParam instanceof Date) {
					result = CommonUtil.parseAsLong(leftParam) <= ((Date)leftParam).getTime();
				}
				
				break;
			}
			
			case SQL_EXPRESSION_GT_N : {
				if(leftParam instanceof Number || rightParam instanceof Number) {
					result = CommonUtil.parseAsDouble(leftParam) > CommonUtil.parseAsDouble(rightParam);
				} else if(leftParam instanceof Date) {
					
					if(rightParam instanceof Date) {
						result = ((Date)leftParam).getTime() > ((Date)rightParam).getTime();
					} else {
						result = ((Date)leftParam).getTime() > CommonUtil.parseAsLong(rightParam);
					}
				} else if(rightParam instanceof Date) {
					result = CommonUtil.parseAsLong(leftParam) > ((Date)leftParam).getTime();
				}
				
				break;
			}
			
			case SQL_EXPRESSION_GE_N : {
				if(leftParam instanceof Number || rightParam instanceof Number) {
					result = CommonUtil.parseAsDouble(leftParam) >= CommonUtil.parseAsDouble(rightParam);
				} else if(leftParam instanceof Date) {
					
					if(rightParam instanceof Date) {
						result = ((Date)leftParam).getTime() >= ((Date)rightParam).getTime();
					} else {
						result = ((Date)leftParam).getTime() >= CommonUtil.parseAsLong(rightParam);
					}
				} else if(rightParam instanceof Date) {
					result = CommonUtil.parseAsLong(leftParam) >= ((Date)leftParam).getTime();
				}
				
				break;
			}
			
			case SQL_EXPRESSION_IN_N : {
				List<Object> rightParamList = CommonUtil.uncheckedListCast(rightParam);
				result = rightParamList == null ? false : rightParamList.contains(leftParam);
				break;
			}
			
			case SQL_EXPRESSION_INS_N : {
				List<Object> rightParamList = CommonUtil.uncheckedListCast(rightParam);
				result = rightParamList == null ? false : rightParamList.contains(String.valueOf(leftParam));
				break;
			}
			
			case SQL_EXPRESSION_NOT_IN_N : {
				List<Object> rightParamList = CommonUtil.uncheckedListCast(rightParam);
				result = rightParamList == null ? true : !rightParamList.contains(leftParam);
				break;
			}
			
			case SQL_EXPRESSION_NOT_INS_N : {
				List<Object> rightParamList = CommonUtil.uncheckedListCast(rightParam);
				result = rightParamList == null ? true : !rightParamList.contains(String.valueOf(leftParam));
				break;
			}
		}
		
		return result;
	}
}
