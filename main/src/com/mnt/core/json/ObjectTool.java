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

package com.mnt.core.json;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.mnt.core.util.CommonUtil;

/**
 * 
 * Parse json style string to Object.
 * 
 * The string should be standard json.
 * 
 * {
 *    _c:"map",
 *    key1:value1,
 *    key2:{
 *    	  _c:"xxx.xxx.XXXObject",
 *        key21:value21,
 *        key22:[]
 *    },
 *    key3:[],
 *    key4:["", "", ""],
 *    key5:"java.util.HashSet"
 * }
 * 
 * @author Peng Peng
 * @date 2011-1-27
 *
 */
public class ObjectTool implements JsonSupport{
	
	private ObjectTool(){
		// empty
	}
	
	public static Object parse(String jsonStr) {
		return parse(jsonStr, false);
	}
	
	public static Object parse(String jsonStr, boolean ignoreError) {
		return jsonStr == null ? null : parse(new IndexableString(jsonStr.trim()), ignoreError);
	}

	private static Object parse(IndexableString source, boolean ignoreError) {
		try{
			if(source.currentChar() == ARR_START) {
				return parseArray(source, ignoreError);
			}else{
				return parseObject(source, ignoreError);
			}
		}catch(Exception e) {
			throw new java.lang.IllegalArgumentException("Invalid json string: " + source);
		} finally {
			source.clear();
		}
	}
	
	private static Object processToken(IndexableString source, boolean isTokenKey){
		source.trimStart();
		
		if(source.moreChars()){
			
			int index = 0;
			char startChar = source.currentChar();
			Object value = null;
			
			// string token
			if((startChar == D_QUOT) || (startChar == S_QUOT)){
				boolean checkNextQuot = true;
				while(++index < source.length()){
					if(source.charEquals(index, startChar)){
						if(checkNextQuot){
							break;
						}else{
							checkNextQuot = true;
						}
					}else if(source.charEquals(index, BACKSLASH)){
						checkNextQuot = !checkNextQuot;
					}else if(!checkNextQuot){ // fix the bug process \n \t etc..
						checkNextQuot = true;
					}
				}
				
				// get the value and move index forward
				value = recoverReservedChars(source.valueBy(1, index++));
			// other value, number, null and other token directly split by comma
			}else{
				if(isTokenKey){
					index = source.indexOf(S_COLON);
					value = source.valueBy(index).trim();
				}else{
					index = source.findNearestIndex(COMMA, OBJ_END, ARR_END);
					value = detectValue(source.valueBy(index).trim());
				}
			}
			
			source.moveIndex(index);
			return value;
		}
		
		return null;
	}
	
	/**
	 * Only works in this class, which the special requirement: ignore the number of "-1"
	 * 
	 * the method is replaced by the IndexableString.findNearestIndex(char... chs)
	 * 
	 * @param nums
	 * @return
	 */
	@SuppressWarnings("unused")
	private static int minNum(int... nums){
		int res = Integer.MAX_VALUE;
		for(int num : nums){
			res = ((res < num) || (num == -1)) ? res : num;
		}
		return res;
	}
	
	private static Object detectValue(String objStr){
		
		if(STATIC_VALUE_TRUE.equals(objStr)) {
			return Boolean.parseBoolean(objStr);
		} else if(STATIC_VALUE_FALSE.equals(objStr)) {
			return Boolean.parseBoolean(objStr);
		} else if(STATIC_VALUE_NULL.equals(objStr)) {
			return null;
		} else if (STATIC_VALUE_UNDEFINED.equals(objStr)) {
			return null;
		}else if(objStr.indexOf(DOT) == -1){
			long val = Long.valueOf(objStr);
			if(val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
				return val;
			} else {
				return (int)val;
			}
		}else {
			double val = Double.valueOf(objStr);
			if(val > Float.MAX_VALUE || val < Float.MIN_VALUE) {
				return val;
			} else {
				int idx = objStr.indexOf('.');
				if(idx != -1) { // double
					int endEIndex = objStr.toUpperCase().indexOf('E');
					if(endEIndex == -1) {
						endEIndex = objStr.length();
						
						if(endEIndex - idx > 7) {
							return val;
						}
						
					} else if(CommonUtil.parseAsInt(objStr.substring(endEIndex + 1)) > 7) { // double
						return val;
					}
				} 
				
				return (float)val;
			}
		}
	}
	
	private static Object parseNextValue(IndexableString source, boolean ignoreError){
		switch(source.currentChar()){
			case OBJ_START:{
				return parseObject(source, ignoreError);
			}
			case ARR_START:{
				return parseArray(source, ignoreError);
			}
			/*case COMMA:{
				source.deleteCharAt(0);
				source.trimStart();
				return parseNextValue(source, ignoreError);
			}*/
			
			// for empty value
			case OBJ_END:
			case ARR_END:{
				return null;
			}
			default: {
				return processToken(source, false);
			}
		}
	}
	
	private static void parseNextValuePair(IndexableString source, Map<String, Object> oMap, boolean ignoreError) {
		
		String key = (String)processToken(source, true);
		source.trimStart();
		source.moveIndex();// delete the next colon
		source.trimStart();
		
		if(JSONTool.CLEAN_JSON_STYLE) {
			oMap.put(key, parseNextValue(source, ignoreError));
		} else {
			if(key.startsWith(COLLECTION_CLASS_PFRIEX)){
				String val = (String)processToken(source, false);
				
				source.moveIndex();// delete the next comma
				source.trimStart();
				
				key = (String)processToken(source, true);
				source.trimStart();
				source.moveIndex();// delete the next colon
				source.trimStart();
				oMap.put(key, parseCollection(source, val, ignoreError));
			}else if(key.startsWith(MAP_CLASS_PFRIEX)){
				String val = (String)processToken(source, false);
				
				source.moveIndex();// delete the next comma
				source.trimStart();
				
				key = (String)processToken(source, true);
				source.trimStart();
				source.moveIndex();// delete the next colon
				source.trimStart();
				oMap.put(key, parseObject(source, val, ignoreError));
			} else {
				oMap.put(key, parseNextValue(source, ignoreError));
			}
		}
	}
	
	private static Object parseObject(IndexableString source, boolean ignoreError){
		return parseObject(source, null, ignoreError);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object parseObject(IndexableString source, String mapClass, boolean ignoreError) {
		
		if(extractNullValue(source) || source.length() == 0) {
			return null;
		} else {
			Map oMap = null;
			
			if(mapClass != null){
				try{
					oMap = (Map<?, ?>)Class.forName(mapClass).newInstance();
				} catch (ClassNotFoundException cnfe){
					if(ignoreError) {
						oMap = new LinkedHashMap();
					} else {
						throw new RuntimeException(cnfe);
					}
				}catch(Exception e){
					//skip any exception
					oMap = new LinkedHashMap();
				}
			}else if(oMap == null){
				oMap = new LinkedHashMap();
			}
			
			source.moveIndex();
			source.trimStart();
			
			char ch = source.currentChar();
			while(ch != OBJ_END){
				parseNextValuePair(source, oMap, ignoreError);
				source.trimStart();
				
				ch = source.currentChar(); 
				if(ch == COMMA){
					source.moveIndex();
					source.trimStart();
				} else if(ch == OBJ_END) {
					break;
				} else {
					throw new RuntimeException("invalid json string, expect comma, but get: " + source.currentChar());
				}
			}
			
			source.moveIndex();
			
			
			if(oMap.containsKey(CLASS_FIELD)) {
				String className = (String)oMap.remove(CLASS_FIELD);
				if(className != null){
					
					try{
						Class<?> c = Class.forName(className);
						
						Object obj = c.newInstance();
						
						if(Date.class.isInstance(obj)){
							
							Object timeVal = oMap.get(VALUE);
							if(timeVal != null){
								((Date)obj).setTime((Long)timeVal);
							}else{
								obj = null;
							}
							
						}else{
							//String methodName = null;
							for(Object fieldName : oMap.keySet()){
								//methodName = METHOD_SET_PREFIX + ((String)fieldName).substring(0, 1).toUpperCase() + ((String)fieldName).substring(1);
								//c.getDeclaredMethod(methodName).invoke(obj, new Object[]{oMap.get(fieldName)});
								try{
									Field f = c.getDeclaredField(String.valueOf(fieldName));
									f.setAccessible(true);
									f.set(obj, oMap.get(fieldName));
								}catch(Exception e){
									e.printStackTrace();
									// skip any exception
								}
							}
						}
						
						oMap.clear();
						return obj;
					}catch(Exception e){
						// while the class does not exists, return the map rather than throw java.lang.ClassNotFoundException.
						return oMap;
					}
				}
			}
			
			return oMap;
		}
	}
	
	public static Object parseArray(IndexableString source, boolean ignoreError) {
		Collection<?> con = parseCollection(source, null, ignoreError);
		
		try {
			return con == null ? null : JSONTool.CLEAN_JSON_STYLE ? con : con.toArray();
		} finally {
			if(!JSONTool.CLEAN_JSON_STYLE) {
				con.clear();
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Collection parseCollection(IndexableString source, String collectionClass, boolean ignoreError) {
		
		if(extractNullValue(source) || source.length() == 0) {
			return null;
		} else {
			Collection collection = null;
			
			if(collectionClass != null) {
				try{
					collection = (Collection<?>) Class.forName(collectionClass).newInstance();
				} catch (ClassNotFoundException cnfe) {
					if(ignoreError) {
						collection = new ArrayList();
					} else {
						throw new RuntimeException(cnfe);
					}
				} catch(Exception e){
					// skip exception
					collection = new ArrayList();
				}
			} else {
				collection = new ArrayList();
			}
			
			// remove "["
			source.moveIndex();
			source.trimStart();
			
			char ch;
			// detect "]" as end character
			while(source.currentChar() != ARR_END){
				collection.add(parseNextValue(source, ignoreError));
				source.trimStart();
				
				ch = source.currentChar();
				if(ch == COMMA) {
					source.moveIndex();
					source.trimStart();
				} else if(ch == ARR_END) {
					break;
				} else {
					throw new RuntimeException("invalid json string, expect comma, but get: " + source.currentChar());
				}
			}
			
			source.moveIndex();
			return collection;
		}
	}
	
	/**
	 * Check if the next value is null, provide for map/collection only
	 * If it is null, the null will be removed.
	 * 
	 * @param source
	 * @return
	 */
	private static boolean extractNullValue(IndexableString source){
		if(source.startsWith(STATIC_VALUE_NULL)){
			source.moveIndex(VALUE_NULL_LEN);
			return true;
		}else {
			return false;
		}
	}
	
	public static String recoverReservedChars(String source) {
		if(source.contains(BACKSLASH_STR_REP)){
			source = source.replace(BACKSLASH_STR_REP, BACKSLASH_STR);
		}
		
		if(source.contains(QUOT_STR_REP)) {
			source = source.replace(QUOT_STR_REP, QUOT_STR);
		}
		
		if(source.indexOf(UNICODE_START) != -1) {
			StringBuilder sb = new StringBuilder();
			int len = source.length();
			int index = 0;
			char ch;
			int flag = 0;
			
			while(index < len) {
				ch = source.charAt(index++);
				
				if(ch == '\\') {
					flag = 1;
				} else if(flag == 1 && Character.toLowerCase(ch) == 'u') {
					if(len - index > 3) {
						
						for(int i = index; i < index + 4; i++) {
							if(!HEX_CHARS.contains(source.charAt(i))) {
								sb.append(UNICODE_START);
								for(int j = index; j <= i; j ++) {
									sb.append(source.charAt(j));
								}
								
								index = i + 1;
								flag = 0;
								
								break;
							}
						}
						
						if(flag == 0) {
							continue;
						} else {
							ch = (char)(Integer.parseInt(source.substring(index, index + 4), 16));
							
							index += 4;
							flag = 0;
							sb.append(ch);
						}
					}
				} else {
					if(flag == 1) {
						flag = 0;
						sb.append('\\');
					}
					
					sb.append(String.valueOf(ch));
				}
			}
			
			source = sb.toString();
		}
		
		return source;
	}
	
	static final Set<Character> HEX_CHARS = new HashSet<Character>();
	static {
		HEX_CHARS.add('a');
		HEX_CHARS.add('b');
		HEX_CHARS.add('c');
		HEX_CHARS.add('d');
		HEX_CHARS.add('e');
		HEX_CHARS.add('f');
		HEX_CHARS.add('A');
		HEX_CHARS.add('B');
		HEX_CHARS.add('C');
		HEX_CHARS.add('D');
		HEX_CHARS.add('E');
		HEX_CHARS.add('F');
		HEX_CHARS.add('0');
		HEX_CHARS.add('1');
		HEX_CHARS.add('2');
		HEX_CHARS.add('3');
		HEX_CHARS.add('4');
		HEX_CHARS.add('5');
		HEX_CHARS.add('6');
		HEX_CHARS.add('7');
		HEX_CHARS.add('8');
		HEX_CHARS.add('9');
	}
}
