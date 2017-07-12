/**
 * $Revision: 1.0
 * $Date: 2013-5-21
 *
 * Copyright (C) 2013-2020 MNT. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.mnt.base.util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class <code>CommonTool</code> is a util methods provider class.
 * 
 * @author Peng Peng
 * @since  2010-11-2
 */
public final class CommonUtil {
	
	private CommonUtil() {
		//empty
	}

	/**
	 * <pre>
	 * Check if the object is empty.
	 * if the object is string, check null and empty string
	 * if the object is collection or Map, check null and empty size
	 * other object, check null.
	 * </pre>
	 * 
	 * @param o	 * @return true if the object is empty base on above rules.
	 */
	public static boolean isEmpty(Object o) {
		
		if(o == null) {
			return true;
		} else if(o instanceof String) {
			return ((String)o).length() == 0;
		} else if(o instanceof Collection<?>) {
			return ((Collection<?>)o).size() == 0;
		} else if(o instanceof Map<?, ?>) {
			return ((Map<?, ?>)o).size() == 0;
		} else if(o instanceof Object[]) {
			return ((Object[])o).length == 0;
		}
		
		return false;
	}
	
	/**
	 * Cast the object to specified type. 
	 * 
	 * It would throw ClassCastException if the type do not match.
	 * 
	 * @param <T> reference type
	 * @param obj cast object
	 * @return Reference type specified object
	 */
	
	@SuppressWarnings("unchecked")
	public static <T> T uncheckedCast(Object obj){
		return (T)obj;
	}
	
	/**
	 * Cast object to map ignore unchecked warning.
	 * 
	 * @param <T> return type
	 * @param obj object to be cast
	 * @return type specified map object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T uncheckedMapCast(Object obj){
		if(obj instanceof Map<?, ?>){
			return (T)obj;
		}else{
			return null;
		}
	}
	
	/**
	 * Cast object to collection ignore unchecked warning.
	 * 
	 * @param <T> return type
	 * @param obj object to be cast
	 * @return type specified collection object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T uncheckedCollectionCast(Object obj){
		if(obj instanceof Collection<?>){
			return (T)obj;
		}else{
			return null;
		}
	}
	
	/**
	 * Cast object to set ignore unchecked warning.
	 * 
	 * @param <T> return type
	 * @param obj object to be cast
	 * @return type specified set object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T uncheckedSetCast(Object obj){
		if(obj instanceof Set<?>){
			return (T)obj;
		}else{
			return null;
		}
	}
	
	/**
	 * Cast object to list ignore unchecked warning.
	 * 
	 * @param <T> return type
	 * @param obj object to be cast
	 * @return type specified list object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T uncheckedListCast(Object obj){
		if(obj instanceof List<?>){
			return (T)obj;
		}else{
			return null;
		}
	}
	
	/**
	 * Cast the object to specified type, if the object type does not match, try primitive type cast with string, otherwise, return null.
	 * 
	 * @param <T> Object type 
	 * @param obj object to cast
	 * @param c specified object class type
	 * @return type specified object
	 */
	public static <T> T castAs(Object obj, Class<T> c){
		return castAs(obj, c, null);
	}
	
	/**
	 * Cast the object to specified type, if the object type does not match, try primitive type cast with string, otherwise 
	 * return the specified default value.
	 * 
	 * @param <T> Object type
	 * @param obj object to cast
	 * @param c specified object class type
	 * @param defaultValue default value to return if the object type does not match 
	 * @return type specified object
	 */
	public static <T> T castAs(Object obj, Class<T> c, Object defaultValue){
		if(c.isInstance(obj)) {
			return c.cast(obj);
		} else if(obj != null && castTypeMap.containsKey(c)) {
			int typeCode = castTypeMap.get(c);
			
			if(typeCode < 7 && defaultValue == null) { // number
				defaultValue = 0;
			}
			
			switch(typeCode) {
				case 1 : return uncheckedCast(parseAsByte(obj, ((Number)defaultValue).byteValue())); 		/*byte*/
				case 2 : return uncheckedCast(parseAsShort(obj, ((Number)defaultValue).shortValue())); 		/*short*/
				case 3 : return uncheckedCast(parseAsInt(obj, ((Number)defaultValue).intValue())); 			/*int*/
				case 4 : return uncheckedCast(parseAsLong(obj, ((Number)defaultValue).longValue())); 		/*long*/
				case 5 : return uncheckedCast(parseAsFloat(obj, ((Number)defaultValue).floatValue())); 		/*float*/
				case 6 : return uncheckedCast(parseAsDouble(obj, ((Number)defaultValue).doubleValue())); 	/*double*/
				case 7 : return uncheckedCast(String.valueOf(obj)); 										/*string*/
				case 8 : return uncheckedCast(parseAsBoolean(obj, Boolean.TRUE.equals(defaultValue))); 		/*boolean*/
				default : {
					// should not get here
					return c.cast(defaultValue);
				}
			}
		} else if(obj instanceof Number && c == Date.class) {
			return uncheckedCast(new Date(((Number)obj).longValue()));
		} else {
			return c.cast(defaultValue);
		}
	}
	
	private static Map<Class<?>, Integer> castTypeMap = new HashMap<Class<?>, Integer>();
	static {
		castTypeMap.put(byte.class, 1);
		castTypeMap.put(Byte.class, 1);
		
		castTypeMap.put(short.class, 2);
		castTypeMap.put(Short.class, 2);
		
		castTypeMap.put(int.class, 3);
		castTypeMap.put(Integer.class, 3);
		
		castTypeMap.put(long.class, 4);
		castTypeMap.put(Long.class, 4);
		
		castTypeMap.put(float.class, 5);
		castTypeMap.put(Float.class, 5);
		
		castTypeMap.put(double.class, 6);
		castTypeMap.put(Double.class, 6);
		
		castTypeMap.put(String.class, 7);
		
		castTypeMap.put(Boolean.class, 8);
	}
	
	/**
	 * Cast object as String, if the type not match, return null.
	 *  
	 * @param obj the object to be cast
	 * @return boolean value
	 */
	public static String castAsString(Object obj){
		return castAs(obj, String.class);
	}
	
	/**
	 * Cast object as boolean, if the type not match, return false.
	 *  
	 * @param obj the object to be cast
	 * @return boolean value
	 */
	public static boolean castAsBoolean(Object obj){
		return castAs(obj, Boolean.class, false);
	}
	
	/**
	 * Cast object as Integer, if the type not match, return 0.
	 * 
	 * @param obj the object to be cast
	 * @return int value
	 */
	public static Integer castAsInteger(Object obj){
		return castAs(obj, Integer.class, 0);
	}
	
	/**
	 * Force cast to the specified object, if type not match, throw RuntimeException
	 * @param <T> parameter
	 * @param obj parameter to be cast
	 * @param c 
	 * @return type specified object
	 */
	public static <T> T forceCast(Object obj, Class<T> c){
		if(c.isInstance(obj)){
			return c.cast(obj);
		}else if(obj == null){
			return null;
		}else{
			throw new RuntimeException("Object: " + obj + " does not match the type: " + c.getCanonicalName());
		}
	}
	
	/**
	 * Force cast object to java.io.Serializable
	 * 
	 * @param obj the object to be cast
	 * @return Serializable object
	 */
	public static Serializable forceCastToSerializable(Object obj){
		return forceCast(obj, Serializable.class);
	}
	

	public static Object parseAsByte(Object obj, byte defaultValue) {
		byte val;
		if(obj instanceof String) {
			try {
				val = Byte.parseByte((String)obj);
			} catch(Exception e) {
				val = defaultValue;
			}
		} else if(obj instanceof Number) {
			val = ((Number)obj).byteValue();
		} else {
			val = defaultValue;
		}
		
		return val;
	}
	
	public static short parseAsShort(Object obj) {
		return parseAsShort(obj, (short)0);
	}
	
	public static short parseAsShort(Object obj, short defaultValue) {
		short val;
		if(obj instanceof String) {
			try {
				val = Short.parseShort((String)obj);
			} catch(Exception e) {
				val = defaultValue;
			}
		} else if(obj instanceof Number) {
			val = ((Number)obj).shortValue();
		} else {
			val = defaultValue;
		}
		
		return val;
	}

	/**
	 * Parse object as int value, while parse failed, return 0
	 * 
	 * @param obj
	 * @return
	 */
	public static int parseAsInt(Object obj) {
		return parseAsInt(obj, 0);
	}
	
	/**
	 * Parse object as int value, while parse failed, return the specified default value
	 * 
	 * @param obj
	 * @param defaultValue
	 * @return
	 */
	public static int parseAsInt(Object obj, int defaultValue) {
		int val;
		if(obj instanceof String) {
			try {
				val = Integer.parseInt((String)obj);
			} catch(Exception e) {
				val = defaultValue;
			}
		} else if(obj instanceof Number) {
			val = ((Number)obj).intValue();
		} else {
			val = defaultValue;
		}
		
		return val;
	}
	
	/**
	 * Parse object as long value, while parse failed, return 0
	 * 
	 * @param obj
	 * @return
	 */
	public static long parseAsLong(Object obj) {
		return parseAsLong(obj, 0);
	}
	
	/**
	 * Parse object as long value, while parse failed, return the specified default value
	 * 
	 * @param obj
	 * @param defaultValue
	 * @return
	 */
	public static long parseAsLong(Object obj, long defaultValue) {
		long val;
		if(obj instanceof String) {
			try {
				val = Long.parseLong((String)obj);
			} catch(Exception e) {
				val = defaultValue;
			}
		} else if(obj instanceof Number) {
			val = ((Number)obj).longValue();
		} else {
			val = defaultValue;
		}
		
		return val;
	}
	
	/**
	 * Parse object as float value, while parse failed, return 0
	 * 
	 * @param obj
	 * @return
	 */
	public static float parseAsFloat(Object obj) {
		return parseAsFloat(obj, 0);
	}
	
	/**
	 * Parse object as float value, while parse failed, return the specified default value
	 * 
	 * @param obj
	 * @param defaultValue
	 * @return
	 */
	public static float parseAsFloat(Object obj, float defaultValue) {
		float val;
		if(obj instanceof String) {
			try {
				val = Float.parseFloat((String)obj);
			} catch(Exception e) {
				val = defaultValue;
			}
		} else if(obj instanceof Number) {
			val = ((Number)obj).floatValue();
		} else {
			val = defaultValue;
		}
		
		return val;
	}
	
	/**
	 * Parse object as double value, while parse failed, return 0
	 * 
	 * @param obj
	 * @return
	 */
	public static double parseAsDouble(Object obj) {
		return parseAsDouble(obj, 0);
	}
	
	/**
	 * Parse object as double value, while parse failed, return the specified default value
	 * 
	 * @param obj
	 * @param defaultValue
	 * @return
	 */
	public static double parseAsDouble(Object obj, double defaultValue) {
		double val;
		if(obj instanceof String) {
			try {
				val = Double.parseDouble((String)obj);
			} catch(Exception e) {
				val = defaultValue;
			}
		} else if(obj instanceof Number) {
			val = ((Number)obj).doubleValue();
		} else {
			val = defaultValue;
		}
		
		return val;
	}
	
	/**
	 * Parse object as boolean value, if parse failed, return false.
	 * 
	 * @param obj
	 * @return
	 */
	public static boolean parseAsBoolean(Object obj) {
		return parseAsBoolean(obj, false);
	}
	
	/**
	 * Parse object as boolean value, if parse failed, return defaultValue.
	 * 
	 * @param obj
	 * @param defaultValue
	 * @return
	 */
	public static boolean parseAsBoolean(Object obj, boolean defaultValue) {
		boolean val;
		if(obj instanceof String) {
			try {
				val = Boolean.valueOf((String)obj);
			} catch(Exception e) {
				val = defaultValue;
			}
		} else if(obj instanceof Boolean) {
			val = ((Boolean)obj);
		} else {
			val = defaultValue;
		}
		
		return val;
	}
	
	/**
	 * Parse object as BigDecimal value, if parse failed, return BigDecimal.ZERO.
	 * 
	 * @param obj
	 * @param defaultValue
	 * @return
	 */
	public static BigDecimal parseAsBigDecimal(Object obj) {
		return parseAsBigDecimal(obj, BigDecimal.ZERO);
	}
	
	/**
	 * Parse object as BigDecimal value, if parse failed, return defaultValue.
	 * 
	 * @param obj
	 * @param defaultValue
	 * @return
	 */
	public static BigDecimal parseAsBigDecimal(Object obj, BigDecimal defaultVal) {
		try {
			return CommonUtil.isEmpty(obj) ? defaultVal : new BigDecimal(String.valueOf(obj));
		} catch(Exception e) {
			return defaultVal;
		}
	}
	
	/**
	 * Deeply clear collection/map/array data
	 * 
	 * @param obj
	 */
	@SuppressWarnings("rawtypes")
	public static void deepClear(Object obj) {
		if(obj instanceof Map){
			Map mapObj = (Map)obj;
			for(Object key : mapObj.keySet()) {
				deepClear(key);
				deepClear(mapObj.get(key));
			}
			
			try{
				mapObj.clear();
			}catch(Exception e){
				//skip it
			}
			
			mapObj = null;
		}else if(obj instanceof Collection) {
			Collection colObj = (Collection)obj;
			for(Object val : colObj){
				deepClear(val);
			}
			
			try{
				colObj.clear();
			}catch(Exception e){
				//skip it
			}
			
			colObj = null;
		} else if(obj instanceof Object[]) {
			Object[] arr = (Object[])obj;
			
			for(Object val : arr){
				deepClear(val);
			}
			
			arr = null;
		}
	}
	
	/**
	 * the simplest way for single JVM unique id generator
	 * @return
	 */
	public synchronized static long generateId(){
		return System.currentTimeMillis() + idCounter ++;
	}
	public static long idCounter = 0;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void listInt2Long(List numberList) {
		if(numberList != null) {
			for(int i = 0; i < numberList.size(); i++ ) {
				numberList.set(i, CommonUtil.parseAsLong(numberList.get(i)));
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void listFloat2Double(List numberList) {
		if(numberList != null) {
			for(int i = 0; i < numberList.size(); i++ ) {
				numberList.set(i, CommonUtil.parseAsDouble(numberList.get(i)));
			}
		}
	}
	
	public static final Map<String, String> TRANSFER_CHAR_MAP = new HashMap<String, String>();
	static {
		TRANSFER_CHAR_MAP.put("\\r", "\r");
		TRANSFER_CHAR_MAP.put("\\n", "\n");
		TRANSFER_CHAR_MAP.put("\\t", "\t");
		TRANSFER_CHAR_MAP.put("\\b", "\b");
		TRANSFER_CHAR_MAP.put("\\f", "\f");
	}
	
	public static final Pattern regex = Pattern.compile("\\\\(r|n|t|b|f)");
	public static String replaceTransferChar(String val) {
		if(val != null) {
			Matcher matcher = regex.matcher(val);
			StringBuffer sb = new StringBuffer();
			while(matcher.find()) {
				matcher.appendReplacement(sb, TRANSFER_CHAR_MAP.get(matcher.group()));
			}
			matcher.appendTail(sb);
			val = sb.toString();
			
			sb.setLength(0);
		}
		
		return val;
	}
	
	public static String concat(Object... strs) {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty(strs)) {
			for(Object str  : strs) {
				sb.append(str);
			}
		}
		
		return sb.toString();
	}
	
	public static String concatWith(String spliteToken, Object... strs) {
		StringBuilder sb = new StringBuilder();
		if(!isEmpty(strs)) {
			for(Object str  : strs) {
				sb.append(str);
				sb.append(spliteToken);
			}
			
			sb.delete(sb.length() - spliteToken.length(), sb.length());
		}
		
		return sb.toString();
	}
}
