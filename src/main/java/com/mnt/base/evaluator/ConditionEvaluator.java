package com.mnt.base.evaluator;

import com.mnt.base.util.CommonUtil;

/**
 * ? : evaluator
 * 
 * @author pengpeng01
 *
 */
public class ConditionEvaluator extends AbstractEvaluator {
	
	private int questionMarkIndex;
	private Evaluator valueEvalutor1;
	private Evaluator valueEvalutor2;
	private LogicEvaluator condition; 

	public ConditionEvaluator(String expression, int questionMarkIndex) {
		super.expression = expression;
		this.questionMarkIndex = questionMarkIndex;
		parse();
	}
	
	public void parse() {
		
		String conditions = expression.substring(0, questionMarkIndex);
		
		String val1;
		String val2;
		
		int deep = 0;
		boolean checkNextCh = true;
		boolean checkNextQuot = true;
		
		char ch;
		
		int index = questionMarkIndex;
		
		checkValsLabel:
		while(index < expression.length()) {
			ch = expression.charAt(++index);
			
			switch(ch) {
			
				case ':' : {
					if(deep == 0 && checkNextCh) {
						break checkValsLabel;
					} 
					
					checkNextQuot = true;
					break;
				}
				
				case '\'' : {
					
					if(checkNextQuot) {
						checkNextCh = !checkNextCh;
					} else {
						checkNextQuot = true;
					}
					break;
				}
				
				case '\\' : {
					
					checkNextQuot = !checkNextQuot;
					break;
				}
				
				case '(' : {
					
					if(checkNextCh) {
						deep ++;
					}
					
					checkNextQuot = true;
					break;
				}
				
				case ')' : {

					if(checkNextCh) {
						deep --;
					}
					
					checkNextQuot = true;
					break;
				}
				
				default : {
					checkNextQuot = true;
					break;
				}
			}
		}
		
		if(index < expression.length()) {
			val1 = expression.substring(questionMarkIndex + 1, index).trim();
			val2 = expression.substring(index + 1).trim();
			
			if(val1.length() > 0 && val2.length() > 0) {
				this.condition = new LogicEvaluator(conditions);
				this.valueEvalutor1 = AbstractEvaluator.parseEvaluator(val1);
				this.valueEvalutor2 = AbstractEvaluator.parseEvaluator(val2);
			} else {
				throw new RuntimeException("invalid ?: expression: " + expression);
			}
		} else {
			throw new RuntimeException("invalid ?: expression: " + expression);
		}
	}
	
	@Override
	public Object eval(Object[] ms) {
		boolean result = CommonUtil.castAsBoolean(condition.eval(ms));
		return format(result ? valueEvalutor1.eval(ms) : valueEvalutor2.eval(ms));
	}
}
