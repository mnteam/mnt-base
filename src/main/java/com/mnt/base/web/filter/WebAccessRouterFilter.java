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

package com.mnt.base.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.util.BaseConfiguration;
import com.mnt.base.util.BeanContext;
import com.mnt.base.web.WebUtils;
import com.mnt.base.web.action.ResponseHandler;
import com.mnt.base.web.action.WebActionControllerManager;


/**
 * Handle request and dispatch the process to action handler manager. 
 * 
 * @author Peng Peng
 * #date 2012-3-22
 *
 *
 */
public class WebAccessRouterFilter implements Filter {

	private final Log log = LogFactory.getLog(getClass());
	
	private WebActionControllerManager webActionControllerManager;
	
	public WebAccessRouterFilter(){
		BeanContext beanContext = BeanContext.getInstance();
		webActionControllerManager = beanContext.getBean("webActionControllerManager", WebActionControllerManager.class);
	}
	
	private static String SKIPPABLE_RES = ".*\\.(jsp|html|htm|css|js|png|jpeg|jpg|gif|swf|txt)$";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest)request;
		HttpServletResponse resp = (HttpServletResponse)response;
		
		WebUtils.setupContext(req, resp);
		
		if(!WebUtils.checkAuth(req, resp)) {
			return;
		}
		
		if(req.getRequestURI().matches(SKIPPABLE_RES)) {
			chain.doFilter(request, response);
			return;
		}
		
		String requestUri = req.getRequestURI();
		if(!webActionControllerManager.dispatchRequest(requestUri, req.getMethod(), new PageResponseHandler(resp, BaseConfiguration.getResponseContentType()))) {
			chain.doFilter(request, response);
		}
		
		WebUtils.clearContext();
	}

	private Object syncObj = new Object();
	
	private class PageResponseHandler implements ResponseHandler{
		
		private HttpServletResponse response;
		protected String contentType;
		
		public PageResponseHandler(HttpServletResponse response, String contentType){
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
			
			synchronized (syncObj) {
				response.setContentType(contentType);
				response.setHeader("Expires", "0");
				response.setHeader("Cache-Control", "no-cache");
				response.setHeader("Pragma", "no-cache");
			}
			
			try {
				response.getWriter().print(contentData);
				response.flushBuffer();
			} catch (IOException e) {
				log.error("fail to output response to client.", e);
			}
		}
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// do nothing
	}
}
