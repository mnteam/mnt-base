package com.mnt.base.evaluator;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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
public interface Evaluator {
	
	public static final String FIRST_LAYER_MAP 		= "M1";
	public static final String SECOND_LAYER_MAP		= "M2";
	public static final String THIRD_LAYER_MAP	 	= "M3";
	public static final String THIRD_LAYER_EXT_MAP 	= "M31"; // for msgpack data process the activity
	public static final String FORTH_LAYER_MAP		= "M4";
	
	String EXPRESSION_MATCH_REGEX_STR 				= "\\[(M.*)\\](\\s*\\{(.*)\\})?";
	Pattern EXPRESSION_MATCH_REGEX 					= Pattern.compile(EXPRESSION_MATCH_REGEX_STR);
	
	String NUMBER_REGEX								= "^(\\-|\\+)?[0-9]+(\\.[0-9]*)?$";
	String BOOLEAN_REGEX							= "^(true|false)$";
	char COMMA										= ',';
	String PERIOD_REGEX								= "\\.";
	String COMMA_REGEX	 							= "\\,";
	String COLON_REGEX	 							= ":";
	String SEMICOLON_REGEX							= ";";
	String STR_POUND								= "#";
	String STR_EXCLAMATORY_MARK						= "!";
	String KEY_DEFAULT								= "default";

	String REGULAR_EXPRESSION					    = "MATCH";

	char SQL_EXPRESSION_START 						= '(';
	char SQL_EXPRESSION_END 						= ')';
	
	String SQL_EXPRESSION_ENDSTR 					= String.valueOf(SQL_EXPRESSION_END);
	String SQL_EXPRESSION_SPACE						= " ";
	
	String SQL_EXPRESSION_AND						= "AND";
	String SQL_EXPRESSION_OR						= "OR";
	String SQL_EXPRESSION_NOT						= "NOT";
	
	String SQL_EXPRESSION_EQ						= "=";
	String SQL_EXPRESSION_EQ2						= "==";
	String SQL_EXPRESSION_NE						= "!=";
	String SQL_EXPRESSION_LT						= "<";
	String SQL_EXPRESSION_LE						= "<=";
	String SQL_EXPRESSION_GT						= ">";
	String SQL_EXPRESSION_GE						= ">=";
	String SQL_EXPRESSION_IN						= "IN";
	String SQL_EXPRESSION_INS						= "INS";
	String SQL_EXPRESSION_NOT_IN					= "NOT IN";
	String SQL_EXPRESSION_NOT_INS					= "NOT INS";
	
	char SQL_NOT_FLAG_CHAR							= '!';
	String SQL_NOT_FLAG								= String.valueOf(SQL_NOT_FLAG_CHAR);
	
	int REGULAR_EXPRESSION_N						= 301;

	int SQL_EXPRESSION_EQ_N							= 101;
	int SQL_EXPRESSION_NE_N							= 102;
	int SQL_EXPRESSION_LT_N							= 103;
	int SQL_EXPRESSION_LE_N							= 104;
	int SQL_EXPRESSION_GT_N							= 105;
	int SQL_EXPRESSION_GE_N							= 106;
	int SQL_EXPRESSION_IN_N							= 107;
	int SQL_EXPRESSION_NOT_IN_N						= 108;
	int SQL_EXPRESSION_INS_N						= 109;
	int SQL_EXPRESSION_NOT_INS_N					= 110;
	
	int SQL_EXPRESSION_AND_N						= 201;
	int SQL_EXPRESSION_OR_N							= 202;
	
	int POW_N										= 100;
	
	int SQL_EXPRESSION_AND_OR						= 2;
	
	char BACKSLASH									= '\\';
	char S_QUOT										= '\'';
	String PIPE										= "|";
	String QUESTION_MARK							= "?";
	
	Map<String, Integer> SQL_EVAL_MAP 		= new HashMap<String, Integer>();
	
	Map<String, Method> FUNC_MAP = new HashMap<String, Method>();
	
	String[] STRING_ARR_TEMP = new String[0];
	
	Object eval(Object[] ms);
	
	Object eval(Object ms);
	
	void setOutputFormatter(OutputFormatter outputFormatter);
}
