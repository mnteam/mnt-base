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

package com.mnt.core.console;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.mnt.core.util.BaseConfiguration;
import com.mnt.core.web.servlets.AccessRouterServlet;


/**
 * <pre>
 * Setup the embed jetty server.
 * 
 * To make the server works, need to address the bean with id: actionControllerManager, @see AccessRouterServlet
 * 
 * Allow the extra servlet map configured as key: servletConfMap.
 * 
 * All beans set in @see BeanContext
 * 
 * All server properties please refer to the config phase @(jetty server related configurations) in server.conf
 * 
 * </pre>
 * 
 * @author Peng Peng
 * @date 2012-3-22
 *
 */
public final class JettyController extends AbstractJettyController implements Runnable {
	
	private static final Log log = LogFactory.getLog(JettyController.class);
	
	private static JettyController instance = new JettyController();
	
	private JettyController() {
		init();
	}
	
	protected void init() {
		log.info("Prepare to startup the Jetty Web Server...");
		super.init();
		
        ServletContextHandler context = new ServletContextHandler(
        		BaseConfiguration.isSessionEnabled() ? ServletContextHandler.SESSIONS : ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
		
		jettyServer.setHandler(context);
		context.addServlet(new ServletHolder(new AccessRouterServlet()), "/*");
        
        super.setupExternalConf(new ExternalConfSetter() {

			@Override
			public void addServlet(Class<HttpServlet> servletClass, String pathSpec) throws Exception {
				HttpServlet servlet = (HttpServlet)(servletClass.newInstance());
				context.addServlet(new ServletHolder(servlet), pathSpec);
			}

			@Override
			public void addFilter(Class<Filter> filterClass, String pathSpec) {
				context.addFilter(filterClass, pathSpec, EnumSet.of(DispatcherType.REQUEST));
			}
        });
	}
	
	@Override
	protected String contextPath() {
		return BaseConfiguration.getServerContextPath();
	}
	
	@Override
	public void run() {
		try {
			jettyServer.start();
			log.info("Jetty Web Server is started and running!");
			jettyServer.join();
		} catch (Exception e) {
			log.error("Fail to startup the jetty web server.", e);
		}
	}

	/**
	 * Start the jetty server with the pre-configured properties.  
	 * 
	 */
	public static void startServer() {
		instance.start();
	}
	
	/**
	 * Stop the jetty server.
	 */
	public static void stopServer() {
		instance.stop();
	}
	
	/**
	 * Get the jetty server state string.
	 * 
	 * @return
	 */
	public static String getServerState() {
		return instance.jettyServer.getState();
	}
}
