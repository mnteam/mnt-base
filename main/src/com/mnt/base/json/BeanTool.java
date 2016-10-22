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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mnt.base.util.CommonUtil;

/**
 * 
 * Parse json style string to bean object.
 * 
 * The string should be standard json.
 * 
 * {
 *    key1:value1,
 *    key2:{
 *        key21:value21,
 *        key22:[]
 *    },
 *    key3:[],
 *    key4:["", "", ""]
 * }
 * 
 * @author Peng Peng
 * @date 2013-11-06
 *
 */
public class BeanTool implements JsonSupport {
	
	public static final Map<Class<?>, Class<?>> INTERFACE_MAPPING_CLASSES = new HashMap<Class<?>, Class<?>>();
	
	private static final Map<Class<?>, Integer> CONSTANT_CLASS_INDEX_MAP = new HashMap<Class<?>, Integer>(); 
	
	private static final int BOOLEAN_INDEX 	= 1;
	private static final int BYTE_INDEX 	= 2;
	private static final int CHAR_INDEX 	= 3;
	private static final int SHORT_INDEX 	= 4;
	private static final int INT_INDEX 		= 5;
	private static final int LONG_INDEX 	= 6;
	private static final int FLOAT_INDEX 	= 7;
	private static final int DOUBLE_INDEX 	= 8;
	private static final int NUMBER_INDEX 	= 9;
	private static final int STRING_INDEX 	= 10;
	private static final int DATE_INDEX 	= 11;
	private static final int OBJECT_INDEX 	= 12;
	
	static {
		// register the default supported class types
		CONSTANT_CLASS_INDEX_MAP.put(Boolean.class, BOOLEAN_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(boolean.class, BOOLEAN_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Byte.class, BYTE_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(byte.class, BYTE_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Character.class, CHAR_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(char.class, CHAR_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Short.class, SHORT_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(short.class, SHORT_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Integer.class, INT_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(int.class, INT_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Long.class, LONG_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(long.class, LONG_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Float.class, FLOAT_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(float.class, FLOAT_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Double.class, DOUBLE_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(double.class, DOUBLE_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(String.class, STRING_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Date.class, DATE_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Number.class, NUMBER_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(CharSequence.class, STRING_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(StringBuilder.class, STRING_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(StringBuffer.class, STRING_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Object.class, OBJECT_INDEX);
		
		INTERFACE_MAPPING_CLASSES.put(Collection.class, ArrayList.class);
		INTERFACE_MAPPING_CLASSES.put(List.class, ArrayList.class);
		INTERFACE_MAPPING_CLASSES.put(Set.class, LinkedHashSet.class);
		INTERFACE_MAPPING_CLASSES.put(CharSequence.class, String.class);
		INTERFACE_MAPPING_CLASSES.put(AbstractSet.class, LinkedHashSet.class);
		
		INTERFACE_MAPPING_CLASSES.put(Map.class, LinkedHashMap.class);
		INTERFACE_MAPPING_CLASSES.put(AbstractMap.class, LinkedHashMap.class);
	}
	
	private BeanTool(){
		// empty
	}
	
	public static <T> T parse(String jsonStr, Class<T> clazz) {
		T t = parse(jsonStr, clazz, false);
		return t;
	}
	
	public static <T> T parse(String jsonStr, Class<T> clazz, boolean ignoreError) { 
		if(jsonStr == null || clazz == null) {
			return null;
		}
		T t = parse(new IndexableString(jsonStr.trim()), clazz, ignoreError);
		return t;
	}
	
	public static<T> T parse(IndexableString source, Class<T> clazz, boolean ignoreError) {
		// default as string:key, object:value
		return parse(source, clazz, String.class, Object.class, ignoreError); 
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object parseValue(Class clazz, int classType, String token, boolean isKey, boolean ignoreError) {
		switch(classType) {
			case BOOLEAN_INDEX : return Boolean.valueOf(token);
			case BYTE_INDEX : return Byte.parseByte(token);
			case CHAR_INDEX : return (token.length() == 0) ? ('\u0000') : token.charAt(0);
			case SHORT_INDEX : return Short.parseShort(token);
			case INT_INDEX : return Integer.parseInt(token);
			case LONG_INDEX : return Long.parseLong(token);
			case FLOAT_INDEX : return Float.parseFloat(token);
			case DOUBLE_INDEX : return Double.parseDouble(token);
			case NUMBER_INDEX : return detectNumberValue(token);
			case STRING_INDEX : {
				if(INTERFACE_MAPPING_CLASSES.containsKey(clazz)) {
					clazz = INTERFACE_MAPPING_CLASSES.get(clazz);
				}
				
				try {
					// value contains the '"'
					return clazz.getConstructor(String.class).newInstance(isKey ? token : token.substring(1, token.length() - 1));
				} catch (Exception e) {
					if(ignoreError) {
						return null;
					} else {
						throw new RuntimeException("error while construct the string type value: " + token + " with class type: " + clazz.getCanonicalName(), e);
					}
				}
			}
			case DATE_INDEX : {
				long timestamp = CommonUtil.parseAsLong(token);
				Object val = null;
				try {
					val = clazz.newInstance();
				} catch (Exception e) {
					if(ignoreError) {
						return null;
					} else {
						throw new RuntimeException("error while create date object instance: " + clazz.getCanonicalName(), e);
					}
				}
				
				((Date)val).setTime(timestamp);
				
				return val;
			} 
			case OBJECT_INDEX : {
				return detectValue(token);
			}
			default : {
				throw new RuntimeException("Unhandled class type: " + classType);
			}
		}
	}
	
	public static<T extends Collection<K>, K> T parseCollection(String jsonStr, Class<T> clazz, Class<K> innerType, boolean ignoreError) {
		if(jsonStr == null || clazz == null || innerType == null) {
			return null;
		}
		
		T t = parseCollection(new IndexableString(jsonStr.trim()), clazz, innerType, ignoreError);
		return t;
	}
	
	public static<T extends Collection<K>, K> T parseCollection(IndexableString source, Class<T> clazz, Class<K> innerType, boolean ignoreError) {
		return parse(source, clazz, null, innerType, ignoreError);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static<T> T parse(IndexableString source, Class<T> clazz, Class innerTypeKey, Class innerTypeValue, boolean ignoreError) {
		
		if(clazz == null) {
			clazz = (Class<T>) Object.class;
		}
		
		if((clazz == Object.class || clazz.isArray()) && source.charEquals(0, ARR_START)) {
			// skip the char '['
			source.moveIndex();
			source.trimStart();

			Class componentClass = clazz.getComponentType();
			boolean isArrayClass = clazz.isArray();
			
			List listVals = new ArrayList<Object>();
			
			boolean parseError = true;
			Object value = null;
			while(true) {
				value = parse(source, isArrayClass ? componentClass : Object.class, ignoreError);
				
				if(!isArrayClass && value != null && componentClass != Object.class) {
					if(componentClass == null) {
						componentClass = value.getClass();
					} else if(componentClass != value.getClass()) { 
						componentClass = Object.class;
					}
				}
				
				listVals.add(value);
				source.trimStart();
				
				// skip the comma
				if(source.charEquals(0, COMMA)) {
					source.moveIndex();
					source.trimStart();
				} else if(source.charEquals(0, ARR_END)) {
					parseError = false;
					break;
				} else {
					if(ignoreError) {
						// skip
					} else {
						throw new RuntimeException("invalid array value in json string: " + source);
					}
				}
			}
			if(parseError) {
				skipUnHandledValue(source, ARR_START, ARR_END);
			}
			// skip the char ']'
			source.moveIndex();
			
			T arrVal = (T)Array.newInstance(componentClass, listVals.size());
			
			for(int i = 0; i < listVals.size(); i++ ) {
				Array.set(arrVal, i, listVals.get(i));
			}
			
			listVals.clear();
			listVals = null;
			
			return arrVal;
		} 
		
		// basic data type
		Integer classType = CONSTANT_CLASS_INDEX_MAP.get(clazz);
		if(classType != null) {
			
			String token = processToken(source, false);
			
			if(token == null || STATIC_VALUE_NULL.equals(token)) {
				
				if(classType < 10) { // primitive type
					switch(classType) {
						case BOOLEAN_INDEX: return (T)Boolean.FALSE; 
						case BYTE_INDEX : return (T)Byte.valueOf((byte)0);
						case CHAR_INDEX : return (T)Character.valueOf('\u0000');
						case SHORT_INDEX : return (T)Short.valueOf((short)0);
						case INT_INDEX : return (T)Integer.valueOf(0);
						case LONG_INDEX : return (T)Long.valueOf(0);
						case FLOAT_INDEX : return (T)Float.valueOf(0);
						case DOUBLE_INDEX : return (T)Double.valueOf(0);
					}
				} 
				// else
				return null;
			} else {
				return (T)parseValue(clazz, classType, token, false, ignoreError);
			}
			
		} /* merge the logic above
		else if(clazz.isArray() && source.charEquals(0, ARR_START)) {
			// skip the char '['
			source.moveIndex();
			source.trimStart();
			
			Class componentClass = clazz.getComponentType();
			List listVals = new ArrayList<Object>();
			boolean parseError = true;
			
			while(true) {
				listVals.add(parse(source, componentClass, ignoreError));
				source.trimStart();
				if(source.charEquals(0, COMMA)) {
					source.moveIndex();
					source.trimStart();
				} else if(source.charEquals(0, ARR_END)) {
					parseError = false;
					break;
				} else {
					if(ignoreError) {
						// skip
					} else {
						throw new RuntimeException("invalid array value in json string: " + source);
					}
				}
			}
			
			if(parseError) {
				skipUnHandledValue(source, ARR_START, ARR_END);
			}
			
			// skip the char ']'
			source.moveIndex();
			
			
			T arrVal = (T)Array.newInstance(componentClass, listVals.size());
			
			for(int i = 0; i < listVals.size(); i++ ) {
				Array.set(arrVal, i, listVals.get(i));
			}
			
			return (T)arrVal;
			//return arrayObj;
		}*/ else {
			if(extractNullValue(source)) {
				return null;
			}
			
			if(Map.class.isAssignableFrom(clazz) && source.charEquals(0, OBJ_START)){
				Map returnVal = null;
				try {
					returnVal = (Map) (INTERFACE_MAPPING_CLASSES.containsKey(clazz) ? 
										INTERFACE_MAPPING_CLASSES.get(clazz).newInstance() 
										: 
										clazz.newInstance());
				} catch (Exception e) {
					if(!ignoreError) {
						throw new RuntimeException("can not create instance for class: " + clazz.getCanonicalName(), e);
					}
				}
				
				// skip the char '{'
				source.moveIndex();
				source.trimStart();
				boolean parseError = true;
				if(returnVal != null) {
					Object key;
					Object value;
					
					while(true) {
						key = processToken(source, true);
						classType = CONSTANT_CLASS_INDEX_MAP.get(innerTypeKey);
						
						if(classType != null) {
							key = parseValue(innerTypeKey, classType, (String)key, true, ignoreError);
							
							source.trimStart();
							// skip the next colon
							source.moveIndex();
							source.trimStart();
							
							value = parse(source, innerTypeValue, ignoreError);
							returnVal.put(key, value);
							
							source.trimStart();
							// skip the next comma
							if(source.currentChar() == COMMA) {
								source.moveIndex();
								source.trimStart();
							} else if(source.currentChar() == OBJ_END) {
								parseError = false;
								break;
							} else {
								if(ignoreError) {
									// skip
								} else {
									throw new RuntimeException("invalid map value in json string: " + source);
								}
							}
						} else { 
							break;
						}
					}
				}

				if(parseError) {
					skipUnHandledValue(source, OBJ_START, OBJ_END);
				}
				
				// skip the char '}'
				source.moveIndex();
				
				return (T)returnVal;
			} else if(Collection.class.isAssignableFrom(clazz) && source.charEquals(0, ARR_START)) {
				Collection returnVal = null;
				
				try {
					returnVal = (Collection) (INTERFACE_MAPPING_CLASSES.containsKey(clazz) ? 
												INTERFACE_MAPPING_CLASSES.get(clazz).newInstance() 
												: 
												clazz.newInstance());
				} catch (Exception e) {
					if(!ignoreError) {
						throw new RuntimeException("can not create instance for class: " + clazz.getCanonicalName(), e);
					} 
				}
				
				// skip the char '['
				source.moveIndex();
				source.trimStart();
				boolean parseError = true;
				if(returnVal != null) {
					Class componentClass = innerTypeValue;
					
					while(true) {
						returnVal.add(parse(source, componentClass, ignoreError));
						source.trimStart();
						if(source.charEquals(0, COMMA)) {
							source.moveIndex();
							source.trimStart();
						} else if(source.charEquals(0, ARR_END)) {
							parseError = false;
							break;
						} else {
							if(ignoreError) {
								// skip
							} else {
								throw new RuntimeException("invalid array value in json string: " + source);
							}
						}
					}
				}
				
				if(parseError) {
					skipUnHandledValue(source, ARR_START, ARR_END);
				}
				// skip the char ']'
				source.moveIndex();
				
				return (T)returnVal;
			} else {
				// java bean process
				T returnVal = null;
				boolean parseError = true;
				
				try {
					returnVal = clazz.newInstance();
				} catch (Exception e) {
					if(!ignoreError) {
						throw new RuntimeException("Error while create instance of object: " + clazz.getCanonicalName(), e);
					} else {
						return null;
					}
				}
				
				// skip the char '{'
				source.moveIndex();
				source.trimStart();
				
				if(returnVal != null) {
					String key;
					Object value = null;
					Field field;
					
					while(true) {
						key = processToken(source, true);
						value = null;
						
						source.trimStart();
						// skip the next colon
						source.moveIndex();
						source.trimStart();
						
						try {
							field = clazz.getDeclaredField(key);
						} catch (Exception e) {
							// skip it
							field = null;
						}
						
						// use field way to process it
						if(field != null && !Modifier.isFinal(field.getModifiers())) {
							Class fieldType = field.getType();
							if(Map.class.isAssignableFrom(fieldType)) { // need to parse the key type and value type
								
								Type genericType = field.getGenericType();
								boolean flag = true;
						        if (genericType instanceof ParameterizedType) {   
						            ParameterizedType parameterizedType = (ParameterizedType)genericType;   
						            Type[] types = parameterizedType.getActualTypeArguments();   

						            if(types.length == 2) {
						            	value = parse(source, fieldType, (Class)types[0], (Class)types[1], ignoreError);
						            	
						            	flag = false;
						            }
						        }
								
						        if(flag) {
						        	value = parse(source, fieldType, ignoreError);
						        }
							} else if(Collection.class.isAssignableFrom(fieldType)) { // need to parse the key type and value type
								
								Type genericType = field.getGenericType();
								boolean flag = true;
						        if (genericType instanceof ParameterizedType) {   
						            ParameterizedType parameterizedType = (ParameterizedType)genericType;   
						            Type[] types = parameterizedType.getActualTypeArguments();   

						            if(types.length == 1) {
						            	value = parse(source, fieldType, null, (Class)types[0], ignoreError);
						            	
						            	flag = false;
						            }
						        }
								
						        if(flag) {
						        	value = parse(source, fieldType, ignoreError);
						        }
							} else {
								value = parse(source, fieldType, ignoreError);
							}
							
							if(value != null) {
								field.setAccessible(true);
								try {
									field.set(returnVal, value);
								} catch (Exception e) {
									if(!ignoreError) {
										throw new RuntimeException("can not set the value: " + value + " for field: " + field.getName(), e);
									}
								}
							}
						} else if(key.length() > 0) {
							// use method way to process it 
							String setMethodName = toSetMethodName(key);
							
							Method[] methods = clazz.getDeclaredMethods();
							Method method = null;
							for(Method m : methods) {
								if(m.getName().equals(setMethodName) && m.getParameterTypes().length == 1 && Modifier.isPublic(m.getModifiers())) { 
									method = m;
									break;
								}
							}
							
							if(method == null) {
								methods = clazz.getMethods();
								for(Method m : methods) {
									if(m.getName().equals(setMethodName) && m.getParameterTypes().length == 1 && Modifier.isPublic(m.getModifiers())) { 
										method = m;
										break;
									}
								}
							}
							
							if(method != null) {
								Class fieldType = method.getParameterTypes()[0];
								
								if(Map.class.isAssignableFrom(fieldType)) { // need to parse the key type and value type
									
									Type genericType = method.getGenericParameterTypes()[0];
									boolean flag = true;
							        if (genericType instanceof ParameterizedType) {   
							            ParameterizedType parameterizedType = (ParameterizedType)genericType;   
							            Type[] types = parameterizedType.getActualTypeArguments();   

							            if(types.length == 2) {
							            	value = parse(source, fieldType, (Class)types[0], (Class)types[1], ignoreError);
							            	
							            	flag = false;
							            }
							        }
									
							        if(flag) {
							        	value = parse(source, fieldType, ignoreError);
							        }
								} else if(Collection.class.isAssignableFrom(fieldType)) { // need to parse the key type and value type
									
									Type genericType = method.getGenericParameterTypes()[0];
									boolean flag = true;
							        if (genericType instanceof ParameterizedType) {   
							            ParameterizedType parameterizedType = (ParameterizedType)genericType;   
							            Type[] types = parameterizedType.getActualTypeArguments();   

							            if(types.length == 1) {
							            	value = parse(source, fieldType, null, (Class)types[0], ignoreError);
							            	
							            	flag = false;
							            }
							        }
									
							        if(flag) {
							        	value = parse(source, fieldType, ignoreError);
							        }
								} else {
									value = parse(source, fieldType, ignoreError);
								}
								
								if(value != null) {
									method.setAccessible(true);
									try {
										method.invoke(returnVal, value);
									} catch (Exception e) {
										if(!ignoreError) {
											throw new RuntimeException("can not set the value: " + value + " for method: " + method.getName() + " with parameter type: " + fieldType.getCanonicalName(), e);
										}
									}
								}
							}
						}
						
						source.trimStart();
						// skip the next comma
						if(source.currentChar() == COMMA) {
							source.moveIndex();
						} else if(source.currentChar() == OBJ_END) {
							break;
						} else {
							if(ignoreError) {
								// skip
							} else {
								throw new RuntimeException("invalid object value in json string: " + source);
							}
						}
					}
					
					if(source.currentChar() == OBJ_END) {
						parseError = false;
					}
				}

				if(parseError) {
					skipUnHandledValue(source, OBJ_START, OBJ_END);
				}
				
				// skip the char '}'
				source.moveIndex();
				
				return (T)returnVal;
			}
		}
	}
	
	private static boolean extractNullValue(IndexableString source){
		if(source.startsWith(STATIC_VALUE_NULL)){
			source.moveIndex(VALUE_NULL_LEN);
			return true;
		}else {
			return false;
		}
	}
	
	private static void skipUnHandledValue(IndexableString source, char endChar, char startChar) {
		if(!source.charEquals(0, ARR_END)) {
			// skip the un-handled value
			int index = 0;
			// string token
			int checkNextEnd = 0;
			boolean checkChar = true;
			boolean checkNextQuot = true;
			char ch;
			
			whileLabel:
			while(index < source.length()){
				ch = source.charAt(index ++);
				switch(ch) {
					case BACKSLASH : {
						checkNextQuot = !checkNextQuot;
						break;
					}
					case D_QUOT : {
						if(checkNextQuot) {
							checkChar = !checkChar;
						}
						
						checkNextQuot = true;
						break;
					}
					
					default : {
						
						if(checkChar) {
							if(ch == startChar) {
								if(checkChar) {
									checkNextEnd ++;
								}
							} else if(ch == endChar) {
								if(checkNextEnd == 0) {
									break whileLabel;
								} else {
									checkNextEnd --;
								}
							}
						}
						
						checkNextQuot = true;
					}
				}
			}
			source.moveIndex(index);
		}
	}

	private static String processToken(IndexableString source, boolean isTokenKey){
		source.trimStart();
		
		if(source.moreChars()){
			
			int index = 0;
			char startChar = source.currentChar();
			String value = null;
			
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
					}
				}
				
				// get the value and move index forward
				value = isTokenKey ? ObjectTool.recoverReservedChars(source.valueBy(1, index++)) : ObjectTool.recoverReservedChars(source.valueBy(0, ++index));
			// other value, number, null and other token directly split by comma
			}else{
				if(isTokenKey){
					index = source.indexOf(S_COLON);
					value = source.valueBy(index).trim();
				}else{
					index = source.findNearestIndex(COMMA, OBJ_END, ARR_END);
					value = source.valueBy(index).trim();
				}
			}
			
			source.moveIndex(index);
			return value;
		}
		
		return null;
	}
	
	private static Object detectNumberValue(String objStr){
		
		if(objStr.indexOf(DOT) == -1){
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
			}  else {
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
	
	private static Object detectValue(String objStr){
		
		if(STATIC_VALUE_TRUE.equals(objStr)) {
			return Boolean.parseBoolean(objStr);
		} else if(objStr.length() > 1 && (objStr.charAt(0) == D_QUOT && objStr.charAt(objStr.length() - 1) == D_QUOT) || (objStr.charAt(0) == S_QUOT && objStr.charAt(objStr.length() - 1) == S_QUOT)) {
			return objStr.substring(1, objStr.length() -1);
		} else if(STATIC_VALUE_FALSE.equals(objStr)) {
			return Boolean.parseBoolean(objStr);
		} else if(STATIC_VALUE_NULL.equals(objStr)) {
			return null;
		} else if (STATIC_VALUE_UNDEFINED.equals(objStr)) {
			return null;
		}else if(objStr.indexOf(DOT) == -1){
			long val;
			try {
				val = Long.valueOf(objStr);
			} catch(NumberFormatException nfe) {
				return objStr; // think as string
			}
			
			if(val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
				return val;
			} else {
				return (int)val;
			}
		}else {
			double val; 
			try {
				val = Double.valueOf(objStr);
			} catch(NumberFormatException nfe) {
				return objStr; // think as string
			}
			
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
	
	private static String toSetMethodName(String fieldName) {
		return new StringBuilder(METHOD_SET_PREFIX).append(fieldName.substring(0, 1).toUpperCase()).append(fieldName.substring(1)).toString();
	}
	
	// code for test only
	public static void main(String[] args) throws Exception {
		//BeanA x = BeanTool.parse("{\"c\":[{\"i\":1,\"b\":true,\"s\":\"test\",\"f\":1.01,\"x\":" + System.currentTimeMillis() + "}],\"x\":{\"mine\":{\"i\":1,\"b\":true,\"s\":\"test\",\"f\":1.01,\"x\":" + System.currentTimeMillis() + "}},\"b\":{\"i\":2,\"b\":false,\"s\":\"ok\",\"f\":2.03,\"x\":" + System.currentTimeMillis() + "}}", BeanA.class);
		
		//List<Bean> x = BeanTool.parseCollection("[{\"i\":1,\"b\":true,\"s\":\"test\",\"f\":1.01,\"x\":" + System.currentTimeMillis() + "},{\"i\":1,\"b\":true,\"s\":\"test\",\"f\":1.01,\"x\":" + System.currentTimeMillis() + "}]", List.class, Bean.class, false);
		
		Bean[] x = BeanTool.parse("[{\"i\":1,\"b\":true,\"s\":\"test\",\"f\":1.01,\"x\":" + System.currentTimeMillis() + "},{\"i\":1,\"b\":true,\"s\":\"test\",\"f\":1.01,\"x\":" + System.currentTimeMillis() + "}]", Bean[].class, false);
		
		System.out.println(Arrays.toString(x));
	}
}

class BeanA {
	
	Map<String, Bean> val;
	Bean b;
	List<Bean> c;

	public Map<String, Bean> getVal() {
		return val;
	}

	public void setVal(Map<String, Bean> val) {
		this.val = val;
	}
	
	public void setX(Map<String, Bean> x) {
		this.val = x;
	}

	@Override
	public String toString() {
		return "BeanA [val=" + val + ", b: " + b + ", c: " + c + "]";
	}
	
	
}

class Bean {
	
	int i;
	boolean b;
	String s;
	float f;
	Date d;
	
	public void setX(Date d) {
		this.d = d;
	}

	@Override
	public String toString() {
		return "Bean [i=" + i + ", b=" + b + ", s=" + s + ", f=" + f
				+ ", d=" + d + "]";
	}
}
