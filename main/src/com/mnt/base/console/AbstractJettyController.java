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

package com.mnt.base.console;

import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.mnt.base.util.BaseConfiguration;
import com.mnt.base.util.BeanContext;
import com.mnt.base.util.CommonUtil;

/**
 * <pre>
 * Base class for JettyController and WebJettyController, providing common methods
 * </pre>
 * 
 * @author Peng Peng
 * @date 2015-07-21
 *
 */
public abstract class AbstractJettyController implements Runnable {
	
	private static final Log log = LogFactory.getLog(AbstractJettyController.class);
	
	protected static final String SERVLET_CONF_MAP = "servletConfMap";
	protected static final String FILTER_CONF_MAP = "filterConfMap";
	
	protected Server jettyServer;
	
	protected interface ExternalConfSetter {
		void addServlet(Class<HttpServlet> servletClass, String pathSpec) throws Exception;
		void addFilter(Class<Filter> filterClass, String pathSpec) throws Exception;
	}
	
	protected void init() {
		QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(BaseConfiguration.getJettyMaxThreadSize());
		
		jettyServer = new Server(threadPool);
		
		String serverHttpHost = BaseConfiguration.getProperty("server_http_host", "0.0.0.0");
		ServerConnector httpConnector = new ServerConnector(jettyServer);
        httpConnector.setHost(serverHttpHost);
        httpConnector.setPort(BaseConfiguration.getServerHTTPPort());
        httpConnector.setIdleTimeout(BaseConfiguration.getIntProperty("server_http_timeout", 30000));
        
        jettyServer.addConnector(httpConnector);
	}
	
	protected void setupExternalConf(final ExternalConfSetter confSetter) {
		
		// setup the external servlets
		Map<String, String> servletConfMap = null;
        try {
        	servletConfMap = CommonUtil.uncheckedMapCast(BeanContext.getInstance().getBean(SERVLET_CONF_MAP, Map.class));
        } catch(Exception e) {
        	// ignore
        }
        
        if(!CommonUtil.isEmpty(servletConfMap)) {
        	
        	Class<HttpServlet> servletClass;
        	String path;
        	
        	for(String clazz : servletConfMap.keySet()) {
        		
        		path = servletConfMap.get(clazz);
        		
        		if(path != null) {
        			try {
        				servletClass = CommonUtil.uncheckedCast(Class.forName(clazz));
            			confSetter.addServlet(servletClass, path);
            			log.info("add external servlet: " + clazz + " for path: " + path);
            		} catch(Exception e) {
            			log.error("fail to construct/setup the external servlet: " +clazz + " for path: " + path);
            		}
        		}
        	}
        }
        
        // setup the external filters
        Map<String, String> filterConfMap = null;
        try {
        	filterConfMap = CommonUtil.uncheckedMapCast(BeanContext.getInstance().getBean(FILTER_CONF_MAP, Map.class));
        } catch(Exception e) {
        	// ignore
        }
        
        if(!CommonUtil.isEmpty(filterConfMap)) {
        	
        	Class<Filter> filterClass;
        	String path;
        	
        	for(String clazz : filterConfMap.keySet()) {
        		path = filterConfMap.get(clazz);
        		if(path != null) {
        			try {
            			filterClass = CommonUtil.uncheckedCast(Class.forName(clazz));
            			confSetter.addFilter(filterClass, path);
            		} catch(Exception e) {
            			log.error("fail to load/setup the filter class" + clazz + " for path: " + path);
            		}
        		}
        	}
        }
	}
	
	protected abstract String contextPath();
	
	protected void asnycStartup(Runnable runnable) {
		new Thread(runnable, "[Wrapper Thread for Jetty Server Startup]: " + contextPath()).start();
	}
	
	protected void start() {
		asnycStartup(this);
	}
	
	protected void stop() {
		log.info("Stopping the Jetty Web Server...");
		try {
			jettyServer.stop();
			log.info("Jetty Web Server is stopped.");
		} catch (Exception e) {
			log.error("error while stop the Jetty Web Server.", e);
		}
	}
}
