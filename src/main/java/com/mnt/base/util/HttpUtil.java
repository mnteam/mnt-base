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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.json.JSONTool;

/**
 * process http request and retrieve the response
 * 
 * @author Peng Peng
 * #date 2012-5-10
 *
 *
 */
public class HttpUtil {
	
	private static final Log log = LogFactory.getLog(HttpUtil.class);
	
	private static int connectionTimeout = -1;
	private static int readTimeout = -1;
	
	public static void setTimeout(int connectionTimeout, int readTimeout) {
		HttpUtil.connectionTimeout = connectionTimeout;
		HttpUtil.readTimeout = readTimeout;
	}
	
	
	
	public static Map<String, Object> processGetRequest(String url){
		return processGetRequest(url, null, null);
	}
	
	public static Map<String, Object> processGetRequest(String url, Map<String, Object> parameters){
		return processGetRequest(url, parameters, null);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> processGetRequest(String url, Map<String, Object> parameters, Map<String, String> headerValue){
		
		String resultJson = processGetRequest2Text(url, parameters, headerValue);
			
		if(resultJson != null){
			try{
				return (Map<String, Object>)JSONTool.convertJsonToObject(resultJson);
			}catch(Exception e){
				log.error("error while process result json string to map object");
			}
		}
		
		return null;
	}
	
	public static Map<String, Object> processPostRequest(String url, Map<String, Object> parameters){
		return processPostRequest(url, parameters, null);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> processPostRequest(String url, Map<String, Object> parameters, Map<String, String> headerValue){
		
		String resultJson = processPostRequest2Text(url, parameters, headerValue);
			
		if(resultJson != null){
			try{
				return (Map<String, Object>)JSONTool.convertJsonToObject(resultJson);
			}catch(Exception e){
				log.error("error while process result json string to map object");
			}
		}
		
		return null;
	}
	
	
	public static String processGetRequest2Text(String url){
		return processGetRequest2Text(url, null, null);
	}
	
	public static String processGetRequest2Text(String url, Map<String, Object> parameters){
		return processGetRequest2Text(url, parameters, null);
	}

	public static String processGetRequest2Text(String url, Map<String, Object> parameters, Map<String, String> headerValue){
		
		HttpURLConnection connection = null;
		
		StringBuilder sb = new StringBuilder(url);
		
		if(!CommonUtil.isEmpty(parameters)){
			if(url.contains("?")){
				sb.append("&");
			}else{
				sb.append("?");
			}
		
			Object value = null;
			for(String key : parameters.keySet()){
				value = parameters.get(key);
				
				if(value instanceof Collection<?> || value instanceof Map<?, ?>){
					try{
						value = JSONTool.convertObjectToJson(value);
					}catch(Exception e){
						log.error("error while convert object to json, " + value);
						value = null;
					}
				}
				
				if(value != null){
					try {// encode the parameters
						sb.append(URLEncoder.encode(key, "utf8")).append("=").append(URLEncoder.encode(value.toString(), "utf8"));
					} catch (UnsupportedEncodingException e) {
						// skip it
					}
					sb.append("&");
				}
			}
			
			sb.deleteCharAt(sb.length() - 1);
		}
		
		try {
			connection = (HttpURLConnection)new URL(sb.toString()).openConnection();
		} catch (IOException e) {
			log.error("Error while process http get request.", e);
		}
		
		if(connection != null){
			try {
				connection.setRequestMethod("GET");
			} catch (ProtocolException e) {
				log.error("should not happen to get error while set the request method as GET.", e);
			}
			
			setTimeout(connection);
			
			if(!CommonUtil.isEmpty(headerValue)){
				for(String key : headerValue.keySet()){
					connection.setRequestProperty(key, headerValue.get(key));
				}
			}
			
			String resultStr = null;
			
			try {
				connection.connect();
				
				resultStr = readData(connection.getInputStream());
			} catch (IOException e) {
				log.error("fail to connect to url: " + sb, e);
			}finally{
				connection.disconnect();
			}

			return resultStr;
		}
		
		return null;
	}
	
	public static String processPostRequest2Text(String url, Map<String, Object> parameters){
		return processPostRequest2Text(url, parameters, null);
	}
	
	public static String processPostRequest2Text(String url, Map<String, Object> parameters, Map<String, String> headerValue){
		
		HttpURLConnection connection = null;
		
		try {
			connection = (HttpURLConnection)new URL(url).openConnection();
		} catch (IOException e) {
			log.error("Error while process http get request.", e);
		}
		
		if(connection != null){
			try {
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
			} catch (ProtocolException e) {
				log.error("should not happen to get error while set the request method as GET.", e);
			}
			
			if(!CommonUtil.isEmpty(headerValue)){
				for(String key : headerValue.keySet()){
					connection.setRequestProperty(key, headerValue.get(key));
				}
			}
			
			// need to specify the content type for post request
			connection.setRequestProperty("Content-Type", "application/json");
			
			setTimeout(connection);
			
			String resultStr = null;
			
			try {
				connection.connect();
				
				if(parameters != null){
					String jsonParams = null;
					
					try{
						jsonParams = JSONTool.convertObjectToJson(parameters);
					}catch(Exception e){
						log.error("error while process parameters to json string.", e);
					}
					
					if(jsonParams != null){
						connection.getOutputStream().write(jsonParams.getBytes());
						connection.getOutputStream().flush();
					}
				}
				
				resultStr = readData(connection.getInputStream());
			} catch (IOException e) {
				log.error("fail to connect to url: " + url, e);
			}finally{
				connection.disconnect();
			}
			
			
			return resultStr;
		}
		
		return null;
	}
	
	private static void setTimeout(HttpURLConnection conn) {
		if(connectionTimeout > 0) {
			conn.setConnectTimeout(connectionTimeout);
		}
		
		if(readTimeout > 0) {
			conn.setReadTimeout(readTimeout);
		}
	}
	
	public static String readData(InputStream in) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
		int len = 0;
		byte[] buff = new byte[1024];
		while((len = in.read(buff)) > 0){
			bos.write(buff, 0, len);
		}
		
		String result = bos.toString();
		bos.close();
		
		return result;
	}
}
