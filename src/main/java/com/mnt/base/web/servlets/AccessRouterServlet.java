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

package com.mnt.base.web.servlets;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.json.JSONTool;
import com.mnt.base.util.BaseConfiguration;
import com.mnt.base.util.BeanContext;
import com.mnt.base.util.CommonUtil;
import com.mnt.base.util.HttpUtil;
import com.mnt.base.web.WebUtils;
import com.mnt.base.web.action.ActionControllerManager;
import com.mnt.base.web.action.InvalidPostHandler;
import com.mnt.base.web.action.ResponseHandler;


/**
 * Handle request and dispatch the process to action handler manager. 
 * 
 * @author Peng Peng
 * #date 2012-3-22
 *
 *
 */
public class AccessRouterServlet extends HttpServlet {

	private static final long serialVersionUID = -855582088339247085L;
	
	private final Log log = LogFactory.getLog(getClass());
	
	private ActionControllerManager actionControllerManager;
	
	private static final String HTTP_GET    = "get";
	private static final String HTTP_DELETE = "delete";
	private static final String HTTP_HEAD   = "head";
	
	
	
	public AccessRouterServlet(){
		BeanContext beanContext = BeanContext.getInstance();
		actionControllerManager = beanContext.getBean("actionControllerManager", ActionControllerManager.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		WebUtils.setupContext(req, resp);
		
		if(!WebUtils.checkAuth(req, resp)) {
			return;
		}
		
		String reqMethod = req.getMethod();
		String requestUri = req.getRequestURI();
		Map<String, Object> parameterMap = null;
		
		String contentType = req.getHeader("content-type");
		
		if(HTTP_GET.equalsIgnoreCase(reqMethod) || HTTP_DELETE.equalsIgnoreCase(reqMethod) || HTTP_HEAD.equalsIgnoreCase(reqMethod) || !"application/json".equalsIgnoreCase(contentType)){
			parameterMap = new LinkedHashMap<String, Object>();
			parameterMap.putAll(req.getParameterMap());
		}else{
			String requestContent = null;
			try{
				requestContent = HttpUtil.readData(req.getInputStream());
			}catch(IOException e){
				log.error("Fail to read the http request content. SKIP the request.", e);
				resp.getOutputStream().close();
				return;
			}
			
			if(!CommonUtil.isEmpty(requestContent)){
				try{
					parameterMap = (Map<String, Object>)JSONTool.convertJsonToObject(requestContent);
				}catch(Exception e){
					if(log.isDebugEnabled()) {
						log.debug("Fail to convert the json string to parameter map. SKIP the request: " + requestContent, e);
					}
					
					InvalidPostHandler invalidPostHandler = null;
					
					try {
						invalidPostHandler = BeanContext.getInstance().getBean("invalidPostHandler", InvalidPostHandler.class);
					} catch(Exception skipe) {
						// skip it
					}
					 
					if(invalidPostHandler != null) {
						try {
							invalidPostHandler.handle(req, resp, requestContent);
						} catch(Exception e1) {
							log.error("error while handle invalid post request.", e);
						}
					} else {
						resp.getOutputStream().close();
					}
					return;
				}
			} else {
				parameterMap = new LinkedHashMap<String, Object>(); 
			}
		}
		
		actionControllerManager.dispatchRequest(requestUri, req.getMethod(), parameterMap, new HTMLResponseHandler(resp, BaseConfiguration.getResponseContentType()));
		CommonUtil.deepClear(parameterMap);
		
		WebUtils.clearContext();
	}
	
	//private Object syncObj = new Object();
	
	private class HTMLResponseHandler implements ResponseHandler{
		
		private HttpServletResponse response;
		protected String contentType;
		
		public HTMLResponseHandler(HttpServletResponse response, String contentType){
			this.response = response;
			this.contentType = contentType;
		}
		
		public void setContentType(String contentType) {
			this.contentType = contentType;
		}
		
		public void response(Object o){
			response(response, o.toString(), this.contentType == null ? "text/html" : this.contentType);
		}
		
		public void response(HttpServletResponse response, String contentData, String contentType) {
			synchronized (response) {
				response.setContentType(contentType);
				response.setHeader("Expires", "0");
				response.setHeader("Cache-Control", "no-cache");
				response.setHeader("Pragma", "no-cache");
			
				try {
					response.getWriter().print(contentData);
					response.getWriter().flush();
				} catch (IOException e) {
					log.error("fail to output response to client.", e);
				}
			}
		}
	}
}
