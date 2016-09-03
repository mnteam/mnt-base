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

package com.mnt.core.web.action.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.core.json.JSONTool;
import com.mnt.core.util.BaseConfiguration;
import com.mnt.core.util.CommonUtil;
import com.mnt.core.util.HttpUtil;
import com.mnt.core.web.WebUtils;
import com.mnt.core.web.action.ActionController;
import com.mnt.core.web.action.ResponseHandler;
import com.mnt.core.web.action.WebActionControllerManager;

/**
 * 
 * @author Peng Peng
 * @date 2012-3-22
 *
 *
 */
public class WebActionControllerManagerImpl extends AbstractActionControllerManager implements WebActionControllerManager{
	
	public WebActionControllerManagerImpl() {
		super(BaseConfiguration.getWebServerContextPath());
	}

	private final Log log = LogFactory.getLog(getClass());
	
	private static final String ERROR_PAGE_URI = BaseConfiguration.getWebServerContextPath() + "/" + BaseConfiguration.getProperty("default_error_page", "error.jsp");
	
	private static final Set<String> HTTP_GET_DEL_HEAD_METHODS = new HashSet<String>();
	static {
		HTTP_GET_DEL_HEAD_METHODS.add("GET");
		HTTP_GET_DEL_HEAD_METHODS.add("DELETE");
		HTTP_GET_DEL_HEAD_METHODS.add("HEAD");
	}
	
	@Override
	public boolean dispatchRequest(String requestUri, String method, ResponseHandler responseHandler) {
		
		boolean handled;
		
		String[] secondLevelPaths = detectSecondLevelPath(requestUri);
		ActionController ac = super.getRequestController(secondLevelPaths);
		
		if(ac != null) {
			
			Map<String, Object> parameters = getParameterMap(ac, method);
			Object responseHolder = super.processRequest(requestUri, method, parameters);
				
			if(responseHolder != null) {
				if(ac.isWebHandler()) {
					
					Map<String, Object> resultMap = CommonUtil.uncheckedMapCast(responseHolder);
					if(!CommonUtil.isEmpty(resultMap)) {
						int responseType = CommonUtil.parseAsInt(resultMap.remove(ActionController.K_RESPONSE_TYPE));
						String responsePath = CommonUtil.castAsString(resultMap.remove(ActionController.K_RESPONSE_PATH));
						
						if(responsePath == null) {
							responsePath = ERROR_PAGE_URI;
						}
						
						if(responseType == ActionController.V_RT_DISPATCH) {
							HttpServletRequest req = WebUtils.getRequest();
							HttpServletResponse resp = WebUtils.getResponse();
							
							for(String key : resultMap.keySet()) {
								req.setAttribute(key, resultMap.get(key));
							}
							
							try {
								WebUtils.getRequest().getRequestDispatcher(responsePath).forward(req, resp);
							} catch (Exception e) {
								log.error("error while dispatch the request to path: " + responsePath, e);
								try {
									WebUtils.getResponse().sendRedirect(ERROR_PAGE_URI);
								} catch (Exception e1) {
									log.error("error while dispatch the request to path[error page]: " + ERROR_PAGE_URI, e);
								}
							}
						} else {
							try {
								WebUtils.getResponse().sendRedirect(responsePath);
							} catch (IOException e) {
								log.error("error while redirect to path: " + responsePath, e);
								
								try {
									WebUtils.getResponse().sendRedirect(ERROR_PAGE_URI);
								} catch (Exception e1) {
									log.error("error while dispatch the request to path[error page]: " + ERROR_PAGE_URI, e);
								}
							}
						}
					}
					
					resultMap.clear();
				} else {
					String responseText = "";
					
					if(responseHolder instanceof String) {
						responseText = CommonUtil.castAsString(responseHolder);
					} else {
						try {
							responseText = JSONTool.convertObjectToJson(responseHolder);
						} catch (Exception e) {
							log.error("fail to convert object to json string: " + responseHolder, e);
						} finally {
							CommonUtil.deepClear(responseHolder);
						}
					}
					
					
					
					responseHandler.response(responseText);
				}
			}
			handled = true;
		}else{
			handled = false;
		}
		
		return handled;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getParameterMap(ActionController actionController, String method) {
		Map<String, Object> parameters = null;
		if(actionController.isWebHandler() ||
				(HTTP_GET_DEL_HEAD_METHODS.contains(method.toUpperCase()))) {
			
			parameters = new LinkedHashMap<String, Object>();
			parameters.putAll(WebUtils.getRequest().getParameterMap());
		} else {
			String requestContent = null;
			try{
				requestContent = HttpUtil.readData(WebUtils.getRequest().getInputStream());
			}catch(IOException e){
				log.error("Fail to read the http request content. SKIP the request.", e);
			}
			
			if(requestContent != null){
				try{
					parameters = (Map<String, Object>)JSONTool.convertJsonToObject(requestContent);
				}catch(Exception e){
					log.error("Fail to convert the json string to parameter map. SKIP the request: " + requestContent, e);
				}
			}
		}
		
		return parameters;
	}
}
