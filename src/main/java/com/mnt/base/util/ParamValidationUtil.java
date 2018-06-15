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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.mnt.base.json.JSONTool;

/**
 * Helper class for request parameter validation
 * 
 * @author Peng Peng
 * #date 2012-5-31
 *
 *
 */
public class ParamValidationUtil {
	
	private Map<String, String> keyPairs;
	
	public static final String KEY 	= "pubkey";
	public static final String SIGN	= "sign";

	public boolean validate(Map<String, Object> params){
		boolean result = false;
		String pubKey = getParameter(params, KEY);
		String privateKey = keyPairs.get(pubKey);
		String sign = getParameter(params, SIGN);
		
		if(!CommonUtil.isEmpty(privateKey) && !CommonUtil.isEmpty(sign)){
			
			List<String> keys = new ArrayList<String>();
			keys.addAll(params.keySet());
			
			keys.remove(SIGN);
			
			Collections.sort(keys);
			
			StringBuffer sb = new StringBuffer();
			
			for(String key : keys){
				sb.append(key.toLowerCase());
				sb.append("=");
				sb.append(getParameter(params, key));
			}
			
			// to lower case
			result = sign.equals(HashUtil.hashByMD5(sb.append(privateKey).toString()));
			
			keys.clear();
			keys = null;
		}
		
		return result;
	}
	
	/**
	 * get string value from parameter map by parameter name.
	 * 
	 * @param parameters
	 * @param parameterName
	 * @return
	 */
	private String getParameter(Map<String, Object> parameters, String parameterName){
		Object paramValue = parameters.get(parameterName);
		
		if(paramValue == null){
			return null;
		}else if(paramValue instanceof String){
			return String.valueOf(paramValue);
		}else if(paramValue instanceof String[] && ((String[])paramValue).length == 1){
			String[] paramValueArr = (String[])paramValue;
			if(CommonUtil.isEmpty((Object)paramValueArr)){
				return null;
			}else{
				return paramValueArr[0];
			}
		}else if(paramValue instanceof Map<?, ?> || paramValue instanceof Collection<?> || paramValue instanceof Object[]){
			return JSONTool.convertObjectToJson(paramValue);
		}else{
			return String.valueOf(paramValue);
		}
	}

	public Map<String, String> getKeyPairs() {
		return keyPairs;
	}

	public void setKeyPairs(Map<String, String> keyPairs) {
		this.keyPairs = keyPairs;
	}
}
