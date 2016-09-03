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

package com.mnt.core.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class <code>MapDataBuilder</code> help the service developer to operate map quickly.
 * 
 * @author Peng Peng
 * @since 2010-10-26
 * 
 */
public class MapUtil {
	
	public static Object getValue(Map<String, Object> dataMap, Object... keys) {

		if (keys != null && keys.length > 0) {
			Object keyValue;
			for (int i = 0, n = keys.length; i < n;) {
				keyValue = dataMap.get(keys[i++]);

				if (i == n) {
					return keyValue;
				} else if (keyValue instanceof Map<?, ?>) {
					dataMap = CommonUtil.uncheckedMapCast(keyValue);
				} else {
					break;
				}
			}
		}

		return null;
	}

	/**
	 * 
	 * @param serviceMap
	 * @param value
	 * @param keys
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> setValue(Map<String, Object> serviceMap, Object value, Object... keys) {
		return setValue(serviceMap, value, false, keys);
	}

	/**
	 * 
	 * @param serviceMap
	 * @param value
	 * @param keys
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Map putValue(Map dataMap, Object value, Object... keys) {
		return setValue(dataMap, value, true, keys);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map setValue(Map dataMap, Object value,
			boolean forceOverride, Object... keys) {
		Map valueMap = dataMap;
		
		if(valueMap == null){
			dataMap = valueMap = new LinkedHashMap();
		}

		if (keys != null && keys.length > 0) {
			Object keyValue;
			Object key;
			for (int i = 0, n = keys.length; i < n;) {
				key = keys[i++];
				keyValue = valueMap.get(key);

				if (i == n) {
					valueMap.put(key, value);
				} else if (keyValue instanceof Map<?, ?>) {
					valueMap = CommonUtil.uncheckedMapCast(keyValue);
				} else if (keyValue == null) {
					valueMap.put(key, new LinkedHashMap());
					valueMap = CommonUtil.uncheckedMapCast(valueMap.get(key));
				} else {
					if (forceOverride) {
						valueMap.put(key, new LinkedHashMap());
						valueMap = CommonUtil.uncheckedMapCast(valueMap.get(key));
					} else {
						throw new IllegalArgumentException(
								"The path key ["
										+ key
										+ "] has been used by another object which type is: "
										+ keyValue.getClass()
												.getCanonicalName());
					}
				}
			}
		}

		return dataMap;
	}
	
	public static boolean containsKey(Map<String, Object> dataMap, Object... keys) {
		Map<String, Object> valueMap = dataMap;
		
		boolean result = false;
		
		if(valueMap != null){
			if (keys != null && keys.length > 0) {
				Object keyValue;
				Object key;
				for (int i = 0, n = keys.length; i < n;) {
					key = keys[i++];
					keyValue = valueMap.get(key);

					if (i == n) {
						result = valueMap.containsKey(key);
					} else if (keyValue instanceof Map<?, ?>) {
						valueMap = CommonUtil.uncheckedMapCast(keyValue);
					} else if (keyValue == null) {
						break;
					}
				}
			}
		}

		return result;
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean containsValue(Map dataMap, Object value, Object... keys) {
		Map valueMap = dataMap;
		
		boolean result = false;
		
		if(valueMap != null){
			if (keys != null && keys.length > 0) {
				Object keyValue;
				Object key;
				for (int i = 0, n = keys.length; i < n;) {
					key = keys[i++];
					keyValue = valueMap.get(key);

					if (i == n) {
						result = keyValue == null ? keyValue == value : keyValue.equals(value);
					} else if (keyValue instanceof Map<?, ?>) {
						valueMap = CommonUtil.uncheckedMapCast(keyValue);
					} else if (keyValue == null) {
						break;
					}
				}
			}
		}

		return result;
	}
	
	public static Object remove(Map<String, Object> dataMap, Object... keys){
		Map<String, Object> valueMap = dataMap;
		
		Object result = false;
		
		if(valueMap != null){
			if (keys != null && keys.length > 0) {
				Object keyValue;
				Object key;
				for (int i = 0, n = keys.length; i < n;) {
					key = keys[i++];
					keyValue = valueMap.get(key);

					if (i == n) {
						result = valueMap.remove(key);
					} else if (keyValue instanceof Map<?, ?>) {
						valueMap = CommonUtil.uncheckedMapCast(keyValue);
					} else if (keyValue == null) {
						break;
					}
				}
			}
		}

		return result;
	}
	
	/**
	 * remove the last key, and check if the parent map is empty, if yes, remove it.
	 *  
	 * @param dataMap
	 * @param keys
	 * @return
	 */
	public static Object removeAndClear(Map<String, Object> dataMap, Object... keys){
		Map<String, Object> valueMap = dataMap;
		
		Object result = false;
		
		List<Map<String, Object>> parentMapList = new ArrayList<Map<String, Object>>();
		parentMapList.add(dataMap);
		
		if(valueMap != null){
			if (keys != null && keys.length > 0) {
				Object keyValue;
				Object key;
				for (int i = 0, n = keys.length; i < n;) {
					key = keys[i++];
					keyValue = valueMap.get(key);

					if (i == n) {
						result = valueMap.remove(key);
						
						for(int x = parentMapList.size() - 1; x > 0; x--) {
							if(valueMap.isEmpty()) {
								valueMap = parentMapList.remove(x - 1);
								valueMap.remove(keys[x - 1]);
							} else {
								break;
							}
						}
						
						parentMapList.clear();
						
					} else if (keyValue instanceof Map<?, ?>) {
						valueMap = CommonUtil.uncheckedMapCast(keyValue);
						parentMapList.add(valueMap);
					} else if (keyValue == null) {
						break;
					}
				}
			}
		}

		return result;
	}
}
