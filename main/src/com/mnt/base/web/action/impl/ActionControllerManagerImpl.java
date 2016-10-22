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

package com.mnt.base.web.action.impl;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.json.JSONTool;
import com.mnt.base.util.BaseConfiguration;
import com.mnt.base.util.CommonUtil;
import com.mnt.base.web.WebUtils;
import com.mnt.base.web.action.ActionController;
import com.mnt.base.web.action.ActionControllerManager;
import com.mnt.base.web.action.ResponseHandler;
/**
 * 
 * @author Peyton Peng
 * @date 2012-3-22
 *
 *
 */
public class ActionControllerManagerImpl extends AbstractActionControllerManager implements ActionControllerManager{
	
	private final Log log = LogFactory.getLog(getClass());
	
	public ActionControllerManagerImpl() {
		super(BaseConfiguration.getServerContextPath());
	}
	
	@Override
	public void dispatchRequest(String requestUri, String method, Map<String, Object> parameters, ResponseHandler responseHandler) {
		
		Object responseHolder = super.processRequest(requestUri, method, parameters);
		
		if(responseHolder != null) {
			String responseText = "";
			
			if(responseHolder instanceof Map) {
				Map<String, Object> resultMap = CommonUtil.uncheckedMapCast(responseHolder);
				int statusCode = CommonUtil.parseAsInt(resultMap.remove(ActionController.K_RESPONSE_CODE), 200);
				WebUtils.getResponse().setStatus(statusCode);
				
				try {
					responseText = JSONTool.convertObjectToJson(resultMap);
				} catch (Exception e) {
					log.error("Fail to convert response object to json string: " + responseHolder, e);
				} finally {
					CommonUtil.deepClear(responseHolder);
				}
			} else if(responseHolder instanceof String) { 
				responseText = CommonUtil.castAsString(responseHolder);
			} else {
				try {
					responseText = JSONTool.convertObjectToJson(responseHolder);
				} catch (Exception e) {
					log.error("Fail to convert response object to json string: " + responseHolder, e);
				} finally {
					CommonUtil.deepClear(responseHolder);
				}
			}
			
			responseHandler.response(responseText);
		}
	}
}
