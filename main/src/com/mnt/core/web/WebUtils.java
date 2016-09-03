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

package com.mnt.core.web;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.mnt.core.util.BaseConfiguration;
import com.mnt.core.util.CommonUtil;

public class WebUtils {
	
	public static ThreadLocal<Map<String, Object>> sessionMapCacher = new ThreadLocal<Map<String, Object>>();
	private static final String AUTH_URI_SKIP_REGEX = BaseConfiguration.getProperty("auth_uri_skip_regex");
	
	public static boolean checkAuth(HttpServletRequest req, HttpServletResponse resp) {
		boolean result;
		if(BaseConfiguration.isDigestAuthEnabled()){
			if((!CommonUtil.isEmpty(AUTH_URI_SKIP_REGEX) && (req.getRequestURI().matches(AUTH_URI_SKIP_REGEX)))) {
				result = true;
			}
			result = DigestAuthenticator.authenticate(req, resp);
		} else {
			result = true;
		}
		
		return result;
	}
	
	public static void setupContext(HttpServletRequest req, HttpServletResponse resp) {
		Map<String, Object> servletObjHolder = WebUtils.servletObjHolder.get();
		
		String webRequestCharset = BaseConfiguration.getProperty("web_request_charset", "UTF-8");
		try {
			req.setCharacterEncoding(webRequestCharset);
			
			resp.reset();
			resp.setCharacterEncoding(webRequestCharset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 

		if(servletObjHolder == null) {
			servletObjHolder = new HashMap<String, Object>(); 
		}
				
		servletObjHolder.put(WebUtils.K_REQUEST, req);
		servletObjHolder.put(WebUtils.K_RESPONSE, resp);
		
		WebUtils.servletObjHolder.set(servletObjHolder);
		
		if(BaseConfiguration.isSessionEnabled()) {
			HttpSession session = req.getSession();
			Map<String, Object> sessionMap = CommonUtil.uncheckedMapCast(session.getAttribute("__sessionMap"));
			if(sessionMap == null) {
				sessionMap = new HashMap<String, Object>();
				session.setAttribute("__sessionMap", sessionMap);
			}
			
			// we just use a independent map for session logic usage.
			WebUtils.sessionMapCacher.set(sessionMap);
		}
	}
	
	public static void clearContext() {
		Map<String, Object> servletObjHolder = WebUtils.servletObjHolder.get();
		if(!CommonUtil.isEmpty(servletObjHolder)) {
			servletObjHolder.clear();
		}
	}
	
	public static String ignoreNull(Object obj) {
		return ignoreNull(obj, "");
	}
	
	public static String ignoreNull(Object obj, String defaultStr) {
		if(obj == null) {
			return defaultStr;
		} else {
			return obj.toString();
		}
	}
	
	public static String ignoreEmpty(Object obj) {
		return ignoreNull(obj, "");
	}
	
	public static String ignoreEmpty(Object obj, String defaultStr) {
		if(CommonUtil.isEmpty(obj)) {
			return defaultStr;
		} else {
			return obj.toString();
		}
	}
	
	public static String toInnerText(Object obj) {
		return ignoreNull(obj).replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	public static Map<String, Object> getSessionMap() {
		return sessionMapCacher.get();
	}
	
	public static ThreadLocal<Map<String, Object>> servletObjHolder = new ThreadLocal<Map<String, Object>>();
	
	public static HttpServletRequest getRequest() {
		return (HttpServletRequest)servletObjHolder.get().get(K_REQUEST);
	}
	
	public static HttpServletResponse getResponse() {
		return (HttpServletResponse)servletObjHolder.get().get(K_RESPONSE);
	}
	
	public static final String K_REQUEST	= "request";
	public static final String K_RESPONSE	= "response";
}
