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


import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Providing method to convert the object to map
 * 
 * @author Peng Peng
 *
 */
public class MapBeanUtil {
	
	public static final Map<Class<?>, Class<?>> INTERFACE_MAPPING_CLASSES = new HashMap<Class<?>, Class<?>>();
	
	private static final Map<Class<?>, Integer> CONSTANT_CLASS_INDEX_MAP = new HashMap<Class<?>, Integer>(); 
	
	private static final String METHOD_GET_PREFIX = "get";
	private static final String METHOD_SET_PREFIX = "set";
	private static final String METHOD_IS_PREFIX  = "is";
	private static final int GET_PREFIX_LEN = METHOD_GET_PREFIX.length();
	private static final int GET_PREFIX_LEN_1 = GET_PREFIX_LEN + 1;
	
	private static final int IS_PREFIX_LEN = METHOD_IS_PREFIX.length();
	private static final int IS_PREFIX_LEN_1 = IS_PREFIX_LEN + 1;
	
	private static final Object[] EMTPY_OBJ_ARR = new Object[]{};
	
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
	private static final int BIGDECIMAL_INDEX = 13;
	
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
		CONSTANT_CLASS_INDEX_MAP.put(Timestamp.class, DATE_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(java.sql.Date.class, DATE_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Number.class, NUMBER_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(CharSequence.class, STRING_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(StringBuilder.class, STRING_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(StringBuffer.class, STRING_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(Object.class, OBJECT_INDEX);
		CONSTANT_CLASS_INDEX_MAP.put(BigDecimal.class, BIGDECIMAL_INDEX);
		
		INTERFACE_MAPPING_CLASSES.put(Collection.class, ArrayList.class);
		INTERFACE_MAPPING_CLASSES.put(List.class, ArrayList.class);
		INTERFACE_MAPPING_CLASSES.put(Set.class, LinkedHashSet.class);
		INTERFACE_MAPPING_CLASSES.put(CharSequence.class, String.class);
		INTERFACE_MAPPING_CLASSES.put(AbstractSet.class, LinkedHashSet.class);
		
		INTERFACE_MAPPING_CLASSES.put(Map.class, LinkedHashMap.class);
		INTERFACE_MAPPING_CLASSES.put(AbstractMap.class, LinkedHashMap.class);
	}
	
	@SuppressWarnings("rawtypes")
	private static Object convertValue(Object value) {
		
		if(value != null) {
			
			if(!CONSTANT_CLASS_INDEX_MAP.containsKey(value.getClass())) {
				
				if(Map.class.isAssignableFrom(value.getClass())) {
					value = convertToDeepMap((Map)value);
				} else if(value.getClass().isArray() || Collection.class.isAssignableFrom(value.getClass())) {
					value = convertToList(value);
				} else {
					value = convertToMap(value);
				}
			}
		}
		
		return value;
	}

	@SuppressWarnings("rawtypes")
	private static Object convertToDeepMap(Map map) {
		
		
		Map<Object, Object> newMap;
		
		if(map != null) {
			
			newMap = new HashMap<Object, Object>();
			
			for(Object key : map.keySet()) {
				newMap.put(convertValue(key), convertValue(map.get(key)));
			}
		} else {
			newMap = null;
		}
		
		return newMap;
	}

	/**
	 * convert bean to map
	 * 
	 * @param bean
	 * @return
	 */
	public static Map<String, Object> convertToMap(Object bean) {
		
		if(bean == null) {
			return null;
		}
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		Class<?> c = bean.getClass();
		
		
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
				
				resultMap.put(fieldName, convertValue(f.get(bean)));
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
				
				resultMap.put(fieldName, convertValue(m.invoke(bean, EMTPY_OBJ_ARR)));
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
		
		return resultMap;
	}
	
	/**
	 * convert array to list
	 * 
	 * @param arr
	 * @return
	 */
	public static List<Object> convertToList(Object arr) {
		
		List<Object> resultList;
		
		if(arr != null) {
			resultList = new ArrayList<Object>();
			
			if(arr instanceof Collection) {
				for(Object o : (Collection<?>)arr) {
					resultList.add(convertValue(o));
				}
			} else if(arr.getClass().isArray()) {
				int len = Array.getLength(arr);
				if(len > 0) {
					for(int i = 0; i < len; i++) {
						resultList.add(convertValue(Array.get(arr, i)));
					}
				}
			}
		} else {
			resultList = null;
		}
		
		return resultList;
	}
	
	@SuppressWarnings({ "rawtypes" })
	private static Object convertValue(Object value, Class clazz) {
		return convertValue(value, clazz, String.class, Object.class);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object convertValue(Object value, Class clazz, Class innerTypeKey, Class innerTypeValue) {
		
		if(value != null) {
			
			if(!CONSTANT_CLASS_INDEX_MAP.containsKey(clazz)) {
				
				if((value instanceof List) && clazz.isArray() ){
					value = convertToArray((List)value, clazz);
				} else if((value instanceof List) && Collection.class.isAssignableFrom(clazz)) {
					value = convertToCollection((List)value, clazz, innerTypeValue);
				} else if(value instanceof Map && Map.class.isAssignableFrom(clazz)) {
					value = convertMapToMap((Map)value, clazz, innerTypeKey, innerTypeValue);
				} else if(value instanceof Map) {
					value = convertToBean((Map)value, clazz);
				}
			} else if(Date.class.isAssignableFrom(clazz) && value instanceof Number) {
				
				value = new Date(((Number)value).longValue());
			}
		}
		
		return value;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object convertMapToMap(Map map, Class mapClass, Class innerTypeKey, Class innerTypeValue) {
		
		Object returnVal = null;
		
		if(map != null && mapClass != null) {
			
			try {
				returnVal = (INTERFACE_MAPPING_CLASSES.containsKey(mapClass) ? 
						INTERFACE_MAPPING_CLASSES.get(mapClass).newInstance() 
						: 
							mapClass.newInstance());
				
				
			} catch (InstantiationException e) {
				// skip it
			} catch (IllegalAccessException e) {
				// skip it
			}
		
			if(returnVal != null) {
				
				for(Object key : map.keySet()) {
					((Map)returnVal).put(convertValue(key, innerTypeKey), convertValue(map.get(key), innerTypeValue));
				}
			}
		}
		
		return returnVal;
	}

	/**
	 * convert map to java bean
	 * 
	 * @param map
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings({ "rawtypes" })
	public static <T> T convertToBean(Map<String, Object> map, Class<T> clazz) {
		T returnVal = null;
		
		if(map != null && clazz != null) {
			
			try {
				returnVal = clazz.newInstance();
			} catch (InstantiationException e) {
				// skip it
			} catch (IllegalAccessException e) {
				// skip it
			}
			
			if(returnVal != null) {
				
				Object value;
				Field field;
				
				for(String key : map.keySet()) {
					
					value = map.get(key);
					
					if(value == null) {
						continue;
					}
					
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
					            	value = convertValue(value, fieldType, (Class)types[0], (Class)types[1]);
					            	
					            	flag = false;
					            }
					        }
							
					        if(flag) {
					        	value = convertValue(value, fieldType);
					        }
						} else if(Collection.class.isAssignableFrom(fieldType)) { // need to parse the key type and value type
							
							Type genericType = field.getGenericType();
							boolean flag = true;
					        if (genericType instanceof ParameterizedType) {   
					            ParameterizedType parameterizedType = (ParameterizedType)genericType;   
					            Type[] types = parameterizedType.getActualTypeArguments();   

					            if(types.length == 1) {
					            	value = convertValue(value, fieldType, null, (Class)types[0]);
					            	
					            	flag = false;
					            }
					        }
							
					        if(flag) {
					        	value = convertValue(value, fieldType);
					        }
						} else {
							value = convertValue(value, fieldType);
						}
						
						if(value != null) {
							field.setAccessible(true);
							try {
								field.set(returnVal, value);
							} catch (Exception e) {
								// skip it
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
						            	value = convertValue(value, fieldType, (Class)types[0], (Class)types[1]);
						            	flag = false;
						            }
						        }
								
						        if(flag) {
						        	value = convertValue(value, fieldType);
						        }
							} else if(Collection.class.isAssignableFrom(fieldType)) { // need to parse the key type and value type
								
								Type genericType = method.getGenericParameterTypes()[0];
								boolean flag = true;
						        if (genericType instanceof ParameterizedType) {   
						            ParameterizedType parameterizedType = (ParameterizedType)genericType;   
						            Type[] types = parameterizedType.getActualTypeArguments();   
	
						            if(types.length == 1) {
						            	value = convertValue(value, fieldType, null, (Class)types[0]);
						            	flag = false;
						            }
						        }
								
						        if(flag) {
						        	value = convertValue(value, fieldType);
						        }
							} else {
								value = convertValue(value, fieldType);
							}
							
							if(value != null) {
								method.setAccessible(true);
								try {
									method.invoke(returnVal, value);
								} catch (Exception e) {
									// skip it
								}
							}
						}
					
					}
					
				}
			}
		}
		
		return returnVal;
	}
	
	/**
	 * convert list&lt;map|object&gt; to specified array
	 * 
	 * @param list
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T convertToArray(List list, Class<T> clazz) {
		
		Class componentType = clazz.getComponentType();
		
		T t = (T)Array.newInstance(componentType, list.size());
		
		for(int i = 0; i < list.size(); i++) {
			Array.set(t, i, convertValue(list.get(i), componentType));
		}
		
		return t;
	}
	
	/**
	 * convert list&lt;map|object&gt; to specified collection
	 * 
	 * @param list
	 * @param clazz
	 * @param componentType
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T convertToCollection(List list, Class<T> clazz, Class componentType) {
		T t = null;
		try {
			t = (T)(INTERFACE_MAPPING_CLASSES.containsKey(clazz) ? 
					INTERFACE_MAPPING_CLASSES.get(clazz).newInstance() 
					: 
						clazz.newInstance());
		} catch (InstantiationException e) {
			// skip
		} catch (IllegalAccessException e) {
			// skip
		}
		
		if(t != null) {
			for(int i = 0; i < list.size(); i++) {
				((Collection)t).add(convertValue(list.get(i), componentType));
			}
		}
		
		return t;
	}
	
	private static String toSetMethodName(String fieldName) {
		return new StringBuilder(METHOD_SET_PREFIX).append(fieldName.substring(0, 1).toUpperCase()).append(fieldName.substring(1)).toString();
	}
	
	
	public static void main(String[] args) {
		
		Bean b = new Bean();
		
		Map<String, Object> map = convertToMap(b);
		
		System.out.println(b);
		
		System.out.println(map);
		
		b = convertToBean(map,  Bean.class);
		
		System.out.println(b);
		
		BeanA ba = new BeanA();
		
		ba.val = new HashMap<String, Bean>();
		ba.val.put("abc", b);
		ba.b = b;
		ba.c = new ArrayList<Bean>();
		ba.c.add(b);
		
		ba.d = new Bean[1];
		ba.d[0] = b;
		
		System.out.println(ba);
		
		Map<String, Object> newBeanAMap = convertToMap(ba);
		
		System.out.println(newBeanAMap);
		
		ba = convertToBean(newBeanAMap, BeanA.class);
		
		System.out.println(ba);
	}
}

class BeanA {
	
	Map<String, Bean> val;
	Bean b;
	List<Bean> c;
	Bean[] d;

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
		return "BeanA [val=" + val + ", b=" + b + ", c=" + c + ", d="
				+ Arrays.toString(d) + "]";
	}
}

class Bean {
	
	int i = 2;
	boolean b = true;
	CharSequence s = "test";
	float f = 1.1f;
	Date d = new Date();
	Timestamp t = new Timestamp(System.currentTimeMillis());
	
	public void setX(Date d) {
		this.d = d;
	}

	@Override
	public String toString() {
		return "Bean [i=" + i + ", b=" + b + ", s=" + s + ", f=" + f
				+ ", d=" + d + ", t=" + t + "]";
	}
}
