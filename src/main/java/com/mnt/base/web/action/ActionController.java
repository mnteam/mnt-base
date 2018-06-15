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

package com.mnt.base.web.action;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mnt.base.util.CommonUtil;

/**
 * Action controller/handler providing with the specified path.
 * 
 * @author Peyton Peng
 * #date 2012-3-22
 *
 *
 */
public abstract class ActionController {
	
	public static final String K_TYPE                  	= "type";
	public static final String K_RESULT 				= "result";
	public static final int INVALID_REQUEST		 		= 400;
	public static final int ACCESS_NO_PRIVILEGE 		= 403;
	public static final int SYSTEM_INTERNAL_ERROR 		= 500;

	// add for web handler support
	public static final String K_RESPONSE_PATH			= "__responsePath";
	public static final String K_RESPONSE_TYPE			= "__responseType";
	
	public static final String K_RESPONSE_CODE			= "__responseCode";
	public static final int V_RT_DISPATCH				= 1;
	public static final int V_RT_REDIRECT				= 2;
	
	private boolean webHandler;
	
	/**
	 * Specify the second level path which handled by this action controller.
	 * 
	 * The path in requestURI:
	 * 
	 * &lt;context&gt;/&lt;path&gt;?params
	 *
	 * Can not be empty if no @ACPath specified.
	 * 
	 * 
	 * @return
	 */
	public String path() {
		return null;
	}
	
	public boolean isWebHandler() {
		return webHandler;
	}
	
	public void setWebHandler(boolean webHandler) {
		this.webHandler = webHandler;
	}
	
	/**
	 * Handle the request with passed parameters.
	 * 
	 * @param method
	 * @param parameters
	 * @return the handler result object.
	 */
	public Object handleRequest(String method, Map<String, Object> parameters) {
		return null;
	}
	
	/**
	 * get string value from parameter map by parameter name.
	 * 
	 * @param parameters
	 * @param parameterName
	 * @return
	 */
	public static String getParameter(Map<String, Object> parameters, String parameterName){
		if(parameters == null) return null;
		
		Object paramValue = parameters.get(parameterName);
		
		if(paramValue == null){
			return null;
		}else if(paramValue instanceof String){
			return String.valueOf(paramValue);
		}else if(paramValue instanceof String[]){
			String[] paramValueArr = (String[])paramValue;
			if(CommonUtil.isEmpty((Object)paramValueArr)){
				return null;
			}else{
				return paramValueArr[0];
			}
		}else{
			return String.valueOf(paramValue);
		}
	}
	
	public static Object getParameterValue(Map<String, Object> parameters, String parameterName){
		Object paramValue = parameters.get(parameterName);
		
		if(paramValue == null){
			return null;
		}else if(paramValue instanceof String){
			return String.valueOf(paramValue);
		}else if(paramValue instanceof String[]){
			String[] paramValueArr = (String[])paramValue;
			if(CommonUtil.isEmpty((Object)paramValueArr)){
				return null;
			}else{
				return paramValueArr[0];
			}
		}else{
			return paramValue;
		}
	}
	
	/**
	 * get string array value from parameter map by parameter name.
	 * 
	 * @param parameters
	 * @param parameterName
	 * @return
	 */
	public static String[] getParameters(Map<String, String[]> parameters, String parameterName){
		return parameters.get(parameterName);
	}

	public static Object getParameterValue(Map<String, Object> parameters, String key, Class<?> clazz) {
		if(clazz == String.class) {
			return getParameter(parameters, key);
		} else if (clazz == Boolean.class || clazz == boolean.class) {
			return "true".equals(getParameter(parameters, key));
		} else if (clazz == int.class || clazz == Integer.class) {
			return CommonUtil.parseAsInt(getParameter(parameters, key));
		} else if (clazz == float.class || clazz == Float.class) {
			return CommonUtil.parseAsFloat(getParameter(parameters, key));
		} else if (clazz == long.class || clazz == Long.class) {
			return CommonUtil.parseAsLong(getParameter(parameters, key));
		} else if (clazz == double.class || clazz == Double.class) {
			return CommonUtil.parseAsDouble(getParameter(parameters, key));
		} else if(clazz == Map.class) {
			return CommonUtil.uncheckedMapCast(getParameterValue(parameters, key));
		} else if(clazz == List.class) {
			return CommonUtil.uncheckedListCast(getParameterValue(parameters, key));
		} else if(clazz == Date.class) { //add date type support
			Object value = getParameter(parameters, key);
			if(!CommonUtil.isEmpty(value)) {
				long ts = CommonUtil.parseAsLong(value);
				if(ts > 0) {
					return new Date(ts);
				} else {
					try {
						return new SimpleDateFormat("yyyy-MM-dd").parse(String.valueOf(value));
					} catch (ParseException e) {
						try {
							return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(String.valueOf(value));
						} catch (ParseException e1) {
							return null;
						}
					}
				}
			}
			
			return null;
		} else {
			return parameters.get(key);
		}
	}
	
	public Object getController() {
		return this;
	}
}
