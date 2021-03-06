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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Providing all configuration items for server.
 * 
 * @author Peng Peng
 * #date 	2012-03-22
 * #updated	2015-07-15
 * 
 */
public class BaseConfiguration {
	
	private static final Log log = LogFactory.getLog(BaseConfiguration.class);
	private static final BaseConfiguration instance	= new BaseConfiguration();
	
	public static final String SERVER_HOME_KEY		= "SERVER_HOME_KEY";
	protected static Properties prop = new Properties();
	private static String serverHome;
	private static String userDir;
	private static boolean homeMode;
	
	private static String baseConfPath;
	
	static {
		populateServerHome();
		loadConfig(ItemKeyDef.V_CONFIG_FILE_PATH);
		loadBaseConf();
	}

	protected BaseConfiguration() {
		// empty
	}
	
	protected BaseConfiguration(String confPath) {
		loadConfig(confPath);
		loadBaseConf();
	}

	public static BaseConfiguration getInstance() {
		return instance;
	}
	
	/**
	 * setup the home path, by default, the program should specify the SERVER_HOME_KEY in system env, and then configuration the home path
	 * in VM parameters.
	 * 
	 * if not specify, the default server home is current folder.
	 */
	private static void populateServerHome() {
		String serverHomeKey = System.getProperty(SERVER_HOME_KEY);
		
		if(!CommonUtil.isEmpty(serverHomeKey)) {
			homeMode = true;
			serverHome = System.getProperty(serverHomeKey);
			
			if (CommonUtil.isEmpty(serverHome)) {
				serverHome = System.getenv(serverHomeKey);
			}
			
			if (serverHome == null) {
				serverHome = "";
			}

			if (!serverHome.endsWith("/") && !serverHome.endsWith("\\")) {
				serverHome += File.separator;
			}
		} else {
			serverHome = "";
			userDir = System.getProperty("user.dir");
			if (!userDir.endsWith("/") && !userDir.endsWith("\\")) {
				userDir += File.separator;
			}
			homeMode = false;
		}
	}
	
	/**
	 * init the configuration properties base on specfied confPath, by default no need to explicitly invoke this method
	 * 
	 * @param confPath
	 */
	protected static void loadConfig(String confPath) {
		InputStream in = getRelativeFileStream(confPath);
		
		if(in != null) {
			try {
				prop.load(in);
			} catch (IOException e) {
				log.error("error while load prop dataa from conf: " + confPath);
			}
		} else if(log.isDebugEnabled()) {
			log.debug("no corresponding conf file, skip to setup configuraiton for file path: " + confPath);
		}
	}
	
	/*private static void setupDefaultConfItems() {
		loadBaseConf();
	}*/
	
	private static void loadBaseConf() {
		
		baseConfPath = getProperty(ItemKeyDef.K_BSAE_CONF_PATH);
		
		if(!CommonUtil.isEmpty(baseConfPath)) {
			Map<String, String> baseConfMap;
			try {
				baseConfMap = loadKeyValuePairs(new FileInputStream(baseConfPath));
			} catch (FileNotFoundException e) {
				log.error("error while load baseConf info: " + baseConfPath);
				baseConfMap = null;
			}
			
			if(!CommonUtil.isEmpty(baseConfMap)) {
				for(String key : baseConfMap.keySet()) {
					if(!prop.containsKey(key)) {
						prop.setProperty(key, baseConfMap.get(key));
					}
				}
			}
		}
	}

	/**
	 * get the string configuration value, if no corresponding config-item, return null.
	 * 
	 * @param key
	 * @return
	 */
	public static String getProperty(String key) {
		return getProperty(key, null);
	}
	
	/**
	 * get the string configuration value, if no corresponding config-item, return defaultValue.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static String getProperty(String key, String defaultValue) {
		String resultVal = null;
		
		if(configProvider != null) {
			if(overrideFlag) {
				resultVal = configProvider.get(key);
				if(resultVal == null) {
					resultVal = prop.getProperty(key);
				}
			} else {
				resultVal = prop.getProperty(key);
				if(resultVal == null) {
					resultVal = configProvider.get(key);
				}
			}
		} else {
			resultVal = prop.getProperty(key);
		}
		
		return CommonUtil.isEmpty(resultVal) ? defaultValue : resultVal;
	}
	
	/**
	 * get the configuration value as float, if no corresponding config-item, return 0.
	 * 
	 * @param key
	 * @return
	 */
	public static int getIntProperty(String key) {
		return getIntProperty(key, 0);
	}
	
	/**
	 * get the configuration value as float, if no corresponding config-item, return defaultValue.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static int getIntProperty(String key, int defaultValue) {
		return CommonUtil.parseAsInt(getProperty(key), defaultValue);
	}
	
	/**
	 * get the configuration value as float, if no corresponding config-item, return 0.
	 * 
	 * @param key
	 * @return
	 */
	public static float getFloatProperty(String key) {
		return getFloatProperty(key, 0);
	}
	
	/**
	 * get the configuration value as float, if no corresponding config-item, return defaultValue.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static float getFloatProperty(String key, float defaultValue) {
		return CommonUtil.parseAsFloat(getProperty(key), defaultValue);
	}
	
	/**
	 * get the configuration value as long, if no corresponding config-item, return 0.
	 * 
	 * @param key
	 * @return
	 */
	public static long getLongProperty(String key) {
		return getLongProperty(key, 0);
	}
	
	/**
	 * get the configuration value as long, if no corresponding config-item, return defaultValue.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static long getLongProperty(String key, long defaultValue) {
		return CommonUtil.parseAsLong(getProperty(key), defaultValue);
	}
	
	/**
	 * get the configuration value as double, if no corresponding config-item, return 0.
	 * 
	 * @param key
	 * @return
	 */
	public static double getDoubleProperty(String key) {
		return getDoubleProperty(key, 0);
	}
	
	/**
	 * get the configuration value as double, if no corresponding config-item, return defaultValue.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static double getDoubleProperty(String key, double defaultValue) {
		return CommonUtil.parseAsDouble(getProperty(key), defaultValue);
	}
	
	/**
	 * get the configuration value as boolean, if no corresponding config-item, return false.
	 * 
	 * @param key
	 * @return
	 */
	public static boolean getBoolProperty(String key) {
		return getBoolProperty(key, false);
	}
	
	/**
	 * parse the configuration value as boolean, if no corresponding config-item, return the default value.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBoolProperty(String key, boolean defaultValue) {
		return CommonUtil.parseAsBoolean(getProperty(key), defaultValue);
	}
	
	public static int getServerShutdownPort() {
		return getIntProperty(ItemKeyDef.K_SERVER_SHUTDOWN_PORT, 47070);
	}

	public static int getWebServerHTTPPort() {
		return getIntProperty(ItemKeyDef.K_WEB_SERVER_HTTP_PORT, 7070);
	}

	public static String getWebServerContextPath() {
		return getProperty(ItemKeyDef.K_WEB_SERVER_CONTEXT_PATH, "");
	}

	public static String getServerContextPath() {
		return getProperty(ItemKeyDef.K_SERVER_CONTEXT_PATH, "");
	}

	public static int getJettyMaxThreadSize() {
		return getIntProperty(ItemKeyDef.K_JETTY_MAX_THREAD_SIZE, 256);
	}

	public static String getAdminUps() {
		return getProperty(ItemKeyDef.K_ADMIN_UPS, "");
	}

	public static int getServerHTTPPort() {
		return getIntProperty(ItemKeyDef.K_SERVER_HTTP_PORT, 7070);
	}

	public static boolean isDigestAuthEnabled() {
		return getBoolProperty(ItemKeyDef.K_DIGEST_AUTH_ENABLED, false);
	}
	
	public static boolean isSessionEnabled() {
		return getBoolProperty(ItemKeyDef.K_SESSION_ENABLED, false);
	}

	public static String getResponseContentType() {
		return getProperty(ItemKeyDef.K_RESPONSE_CONTENT_TYPE, "text/json");
	}
	
	public static String getConfFolderName() {
		return getProperty(ItemKeyDef.K_CONF_FOLDER_NAME, "conf");
	}
	
	public static boolean isHomeMode() {
		return homeMode;
	}
	
	public static String getServerHome() {
		return serverHome;
	}
	
	public static String getServerHome(boolean defaultUserDir) {
		return homeMode ? serverHome : userDir;
	}
	
	public static Map<String, String> loadKeyValuePairs(InputStream in) {
		Map<String, String> keyPairs = new HashMap<String, String>();
		
		if(in != null) {
			Properties prop = new Properties();
			try {
				prop.load(in);
			} catch (IOException e) {
				log.error("error while load keyvalue pairs to properties.", e);
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					log.error("error while close the properties file: ", e);
				}
			}
			
			for(String key : prop.stringPropertyNames()) {
				keyPairs.put(key, prop.getProperty(key));
			}
		}
		
		return keyPairs;
	}
	
	/**
	 * Load the properties key value pairs to hash map.
	 * the properties configuration path is based on specified server home. 
	 * 
	 * @param confPath
	 * @return
	 */
	public static Map<String, String> loadKeyValuePairs(String confPath) {
		return loadKeyValuePairs(getRelativeFileStream(confPath));
	}
	
	
	public static interface ConfigProvider {
		String get(String key);
	}
	
	private static ConfigProvider configProvider;
	private static boolean overrideFlag = false;
	
	public static void applyExtenalConfig(ConfigProvider cp, final boolean override) {
		BaseConfiguration.configProvider = cp;
		BaseConfiguration.overrideFlag = override;
	}
	
	/**
	 * return the conf related file input stream, if the server conf is home configured mode, use the home related mode,
	 * else use the class path mode
	 * 
	 * @param confPath
	 * @return
	 */
	public static InputStream getRelativeFileStream(String confPath) {
		InputStream in = null;
		if(homeMode) {
			try {
				in = new FileInputStream(new StringBuilder(getServerHome()).append(confPath).toString());
			} catch (FileNotFoundException e) {
				in = null;
			}
			
			if(in == null) {
				try {
					in = new FileInputStream(new StringBuilder(getServerHome()).append(getConfFolderName()).append(File.separator).append(confPath).toString());
				} catch (FileNotFoundException e) {
					in = null;
				}
			}
		} else {
			in = BaseConfiguration.class.getClassLoader().getResourceAsStream(confPath);
		}
		
		return in;
	}

	private interface ItemKeyDef {
		String K_BSAE_CONF_PATH 		= "base_conf_path";
		String K_SERVER_HTTP_PORT 		= "server_http_port";
		String K_WEB_SERVER_HTTP_PORT 	= "web_server_http_port";
		String K_SERVER_SHUTDOWN_PORT 	= "server_shutdown_port";
		String K_SERVER_CONTEXT_PATH 	= "server_context_path";
		String K_WEB_SERVER_CONTEXT_PATH= "web_server_context_path";
		String K_JETTY_MAX_THREAD_SIZE 	= "jetty_max_thread_size";
		String K_ADMIN_UPS 				= "admin_ups";
		String K_DIGEST_AUTH_ENABLED 	= "digest_auth_enabled";
		String K_SESSION_ENABLED 		= "session_enabled";
		String V_CONFIG_FILE_PATH 		= "server.conf";
		String K_RESPONSE_CONTENT_TYPE 	= "response_content_type";
		String K_CONF_FOLDER_NAME		= "conf_folder_name";
	}
}
