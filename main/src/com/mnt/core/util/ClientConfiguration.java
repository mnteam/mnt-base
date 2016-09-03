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

package com.mnt.core.util;

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
 * @date 	2016-02-11
 * @updated	2016-02-11
 * 
 */
public class ClientConfiguration {
	
	private static final Log log = LogFactory.getLog(ClientConfiguration.class);
	private static final ClientConfiguration instance = new ClientConfiguration();
	protected static Properties prop = new Properties();
	
	static {
		loadConfig(ItemKeyDef.V_CONFIG_FILE_PATH);
	}

	protected ClientConfiguration() {
		// empty
	}
	
	protected ClientConfiguration(String confPath) {
		loadConfig(confPath);
	}

	public static ClientConfiguration getInstance() {
		return instance;
	}
	
	/**
	 * get the string configuration value, if no corresponding config-item, return null.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static String getProperty(String key) {
		return prop.getProperty(key);
	}
	
	/**
	 * get the string configuration value, if no corresponding config-item, return defaultValue.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static String getProperty(String key, String defaultValue) {
		String resultVal = prop.getProperty(key);
		return CommonUtil.isEmpty(resultVal) ? defaultValue : resultVal;
	}
	
	/**
	 * get the configuration value as float, if no corresponding config-item, return 0.
	 * 
	 * @param key
	 * @param defaultValue
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
		return CommonUtil.parseAsInt(prop.getProperty(key), defaultValue);
	}
	
	/**
	 * get the configuration value as float, if no corresponding config-item, return 0.
	 * 
	 * @param key
	 * @param defaultValue
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
		return CommonUtil.parseAsFloat(prop.getProperty(key), defaultValue);
	}
	
	/**
	 * get the configuration value as long, if no corresponding config-item, return 0.
	 * 
	 * @param key
	 * @param defaultValue
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
		return CommonUtil.parseAsLong(prop.getProperty(key), defaultValue);
	}
	
	/**
	 * get the configuration value as double, if no corresponding config-item, return 0.
	 * 
	 * @param key
	 * @param defaultValue
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
		return CommonUtil.parseAsDouble(prop.getProperty(key), defaultValue);
	}
	
	/**
	 * get the configuration value as boolean, if no corresponding config-item, return false.
	 * 
	 * @param key
	 * @param defaultValue
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
		return CommonUtil.parseAsBoolean(prop.getProperty(key), defaultValue);
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
	
	/**
	 * Load the properties key value pairs to hash map.
	 * the properties configuration path is based on specified server home. 
	 * 
	 * @param configPath
	 * @return
	 */
	public static Map<String, String> loadKeyValuePairs(String confPath) {
		return loadKeyValuePairs(getRelativeFileStream(confPath));
	}
	
	/**
	 * return the conf related file input stream, if the server conf is home configured mode, use the home related mode,
	 * else use the class path mode
	 * 
	 * @param confPath
	 * @return
	 */
	public static InputStream getRelativeFileStream(String confPath) {
		InputStream in = ClientConfiguration.class.getClassLoader().getResourceAsStream(confPath);
		
		return in;
	}
	
	private interface ItemKeyDef {
		String V_CONFIG_FILE_PATH 		= "client.conf";
	}
}
