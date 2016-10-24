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

import com.mnt.base.util.BaseConfiguration;
import com.mnt.base.util.CommonUtil;
import com.mnt.base.web.DigestAuthenticator;

public class AuthFilter implements Filter{

	private static final String AUTH_URI_SKIP_REGEX = BaseConfiguration.getProperty("auth_uri_skip_regex");

	@Override
	public void destroy() {
		// skip it
	}

	@Override
	public void doFilter(ServletRequest arg0, ServletResponse arg1,
			FilterChain arg2) throws IOException, ServletException {
		
		if(!CommonUtil.isEmpty(AUTH_URI_SKIP_REGEX) && ((HttpServletRequest)arg0).getRequestURI().matches(AUTH_URI_SKIP_REGEX)) {
			arg2.doFilter(arg0, arg1);
		} else if(DigestAuthenticator.authenticate((HttpServletRequest)arg0, (HttpServletResponse)arg1)){
			arg2.doFilter(arg0, arg1);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// skip it
	}
}
