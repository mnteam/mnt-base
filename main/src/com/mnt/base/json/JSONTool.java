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

package com.mnt.base.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mnt.base.util.CommonUtil;

/**
 * <pre>
 * JSON parse tool.
 * 
 * providing the way to parse json string with object|bean.
 * 
 * object -> json
 * json -> java bean
 * json -> java map
 * json -> array
 * 
 * </pre>
 * @author Peng Peng
 *
 */
public class JSONTool implements JsonSupport{
	
	public static final boolean CLEAN_JSON_STYLE = true;
	
	private JSONTool(){
		
	}
	
	private static void convertMapToJson(Map<Object, Object> map, StringBuilder sb) {
		sb.append(OBJ_START);
		
		if(!map.isEmpty()){
		
			for(Object key : map.keySet()){
				convertObjectToJson(String.valueOf(key), map.get(key), sb);
				sb.append(COMMA);
			}
			
			//remove the last comma
			sb.deleteCharAt(sb.length() - 1);
		}
		
		sb.append(OBJ_END);
	}

	private static void convertArrayToJson(Object arr, StringBuilder sb) {
		sb.append(ARR_START);
		
		int len = Array.getLength(arr);
		
		if(len > 0){
			Object val;
			for(int i = 0; i < len; i++){
				val = Array.get(arr, i);
				convertObjectToJson(val, sb);
				sb.append(COMMA);
			}
			
			//remove the last comma
			sb.deleteCharAt(sb.length() - 1);
		}
		
		sb.append(ARR_END);
	}
	
	private static void insertClassDesc(StringBuilder sb, int offset, String key, String value){
		StringBuilder mClassDesc = new StringBuilder();
		
		mClassDesc.append(D_QUOT);
		mClassDesc.append(key);
		mClassDesc.append(D_QUOT);
		
		mClassDesc.append(COLON);
		
		mClassDesc.append(D_QUOT);
		mClassDesc.append(value);
		mClassDesc.append(D_QUOT);
		
		mClassDesc.append(COMMA);
		
		sb.insert(offset, mClassDesc);
		
		mClassDesc.setLength(0);
	}

	private static void convertCollectionToJson(Collection<?> mapVal, StringBuilder sb) {
		sb.append(ARR_START);
		
		if(!mapVal.isEmpty()){
		
			for(Object val : mapVal){
				convertObjectToJson(val, sb);
				sb.append(COMMA);
			}
			
			//remove the last comma
			sb.deleteCharAt(sb.length() - 1);
		}
		
		sb.append(ARR_END);
	}
	
	private static void convertNumberToJson(Number obj, StringBuilder sb) {
		sb.append(obj);
	}
	
	private static void convertBeanToJson(Object obj, StringBuilder sb){

		if(obj instanceof Date){
			
			if(!CLEAN_JSON_STYLE){
				sb.append(OBJ_START);
				sb.append(D_QUOT);
				sb.append(VALUE);
				sb.append(D_QUOT);
				
				sb.append(COLON);
			}
			
			sb.append(((Date)obj).getTime());
			
			if(!CLEAN_JSON_STYLE){
				sb.append(OBJ_END);
			}
		}else{
			sb.append(OBJ_START);
			Class<?> c = obj.getClass();
			
			if(!CLEAN_JSON_STYLE){
				insertClassDesc(sb, sb.length(), CLASS_FIELD, c.getName());
			}
			
			Set<String> fieldNameSet = new HashSet<String>();
			Field[] fields = c.getDeclaredFields(); 
			String fieldName;
			int modifiers;
			for(Field f : fields){
				
				modifiers = f.getModifiers();
				fieldName = f.getName();
				
				if(f.isEnumConstant() || 
					Modifier.isFinal(modifiers) || 
					Modifier.isTransient(modifiers)){
					continue;
				}
				
				f.setAccessible(true);
				try {
					convertObjectToJson(fieldName, f.get(obj), sb);
					sb.append(COMMA);
					fieldNameSet.add(fieldName);
				} catch (IllegalArgumentException e) {
					// skip any error
				} catch (IllegalAccessException e) {
					// skip any error
				}
			}
			
			Method[] methods = c.getDeclaredMethods();
			String methodName;
			for(Method m : methods) { 
				methodName = m.getName();
				modifiers = m.getModifiers();
				if(!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers)) {
					continue;
				}
				
				if(methodName.startsWith(METHOD_IS_PREFIX) && (m.getReturnType() == Boolean.class || m.getReturnType() == boolean.class)) {
					fieldName = Character.toLowerCase(methodName.charAt(IS_PREFIX_LEN)) + methodName.substring(IS_PREFIX_LEN_1);
				} else if(methodName.startsWith(METHOD_IS_PREFIX)) {
					fieldName = Character.toLowerCase(methodName.charAt(GET_PREFIX_LEN)) + methodName.substring(GET_PREFIX_LEN_1);
				} else {
					continue;
				}
				
				if(fieldNameSet.contains(fieldName) || fieldName.length() == 0) {
					continue;
				}
				
				m.setAccessible(true);
				try {
					convertObjectToJson(fieldName, m.invoke(obj, EMTPY_OBJ_ARR), sb);
					sb.append(COMMA);
				} catch (IllegalArgumentException e) {
					// skip any error
				} catch (IllegalAccessException e) {
					// skip any error
				} catch (InvocationTargetException e) {
					// skip any error
				}
			}
			
			fieldNameSet.clear();
			fieldNameSet = null;
			
			if(sb.charAt(sb.length() - 1) == COMMA){
				sb.deleteCharAt(sb.length() - 1);
			}
			
			sb.append(OBJ_END);
		}
		
		//sb.append(OBJ_END);
	}
	
	private static void convertObjectToJson(Object obj, StringBuilder sb){
		convertObjectToJson(obj, sb, null);
	}
	
	@SuppressWarnings("unused")
	private static void convertObjectToJson(Object obj, StringBuilder sb, String key){
		
		if(obj instanceof Number){
			convertNumberToJson((Number)obj, sb);
		}else if(obj instanceof Character){
			sb.append(obj);
		}else if(obj instanceof Boolean){
			sb.append(obj);
		}else if(obj instanceof CharSequence){
			sb.append(D_QUOT);
			sb.append(replaceReservedChars(obj.toString()));
			sb.append(D_QUOT);
		}else if(obj instanceof Map<?, ?>){
			
			if(!CLEAN_JSON_STYLE && key != null){
				insertClassDesc(sb, sb.length() - key.length() - 3, MAP_CLASS_PFRIEX + key, obj.getClass().getName());
			}
			
			Map<Object, Object> mapVal = CommonUtil.uncheckedMapCast(obj);
			convertMapToJson(mapVal, sb);
		}else if(obj instanceof Collection<?>){
			if(!CLEAN_JSON_STYLE && key != null){
				insertClassDesc(sb, sb.length() - key.length() - 3, COLLECTION_CLASS_PFRIEX + key, obj.getClass().getName());
			}
			
			Collection<?> mapVal = CommonUtil.uncheckedCollectionCast(obj);
			convertCollectionToJson(mapVal, sb);
		}else if(obj == null){
			sb.append(STATIC_VALUE_NULL);
		}else if(obj.getClass().isArray()){
			convertArrayToJson(obj, sb);
		}else{
			convertBeanToJson(obj, sb);
		}
	}
	
	private static void convertObjectToJson(String key, Object obj, StringBuilder sb){
		
		key = replaceReservedChars(key);
		
		sb.append(D_QUOT);
		sb.append(key);
		sb.append(D_QUOT);
		sb.append(COLON);
		
		convertObjectToJson(obj, sb, key);
	}
	
	public static String replaceReservedChars(String source) {
		if(source.contains(BACKSLASH_STR)){
			source = source.replace(BACKSLASH_STR, BACKSLASH_STR_REP);
		}
		
		if(source.contains(QUOT_STR)) {
			source = source.replace(QUOT_STR, QUOT_STR_REP);
		}
		
		StringBuilder sb = new StringBuilder();
		for(char ch : source.toCharArray()) {
			if(ch < SPACE) {
				if(ch < 16) {
					sb.append("\\u000");
				} else {
					sb.append("\\u00");
				}
				sb.append(Integer.toHexString(ch).toUpperCase());
			} else {
				sb.append(ch);			
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * convert object|array to json string
	 * 
	 * @param o
	 * @return
	 */
	public static String convertObjectToJson(Object o){
		StringBuilder sb = new StringBuilder();
		convertObjectToJson(o, sb);
		return sb.toString();
	}
	
	/**
	 * 
	 * convert json string to java map or list(non-array|bean support)
	 * 
	 * @param jsonStr
	 * @return
	 */
	public static Object convertJsonToObject(String jsonStr){
		return convertJsonToObject(jsonStr, false);
	}
	
	/**
	 * convert json string to java map or list(non-array|bean support)
	 * 
	 * @param jsonStr
	 * @param ignoreError
	 * @return
	 */
	public static Object convertJsonToObject(String jsonStr, boolean ignoreError){
		return jsonStr != null ? ObjectTool.parse(jsonStr, ignoreError) : null;
	}
	
	/**
	 * 
	 * convert json to specified java bean(including array, java map, list etc.) obejct.
	 * 
	 * @param jsonStr
	 * @param clazz
	 * @param ignoreError
	 * @return
	 */
	public static<T> T convertJsonToObject(String jsonStr, Class<T> clazz, boolean ignoreError){
		return jsonStr != null ? BeanTool.parse(jsonStr, clazz, ignoreError) : null;
	}
	
	/**
	 * convert json to specified java bean(including array, java map, list etc.) obejct.
	 * 
	 * @param jsonStr
	 * @param clazz
	 * @param innerType
	 * @param ignoreError
	 * @return
	 */
	public static<T extends Collection<K>, K> T convertJsonToArray(String jsonStr, Class<T> clazz, Class<K> innerType, boolean ignoreError){
		return jsonStr != null ? BeanTool.parseCollection(jsonStr, clazz, innerType, ignoreError) : null;
	}
	
	// test only
	public static void main(String[] args) {
		String data = "{}";
		
		
		Object json = JSONTool.convertJsonToObject(data);
		
		System.out.println(json);
	}
}
