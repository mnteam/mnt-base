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

package com.mnt.base.stream.server;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.netty.Connection;


/**
 * manage valid connection
 * 
 * @author Peng Peng
 * #date 2012-5-9
 *
 *
 */
public class ConnectionManager {
	
	private static final Log log = LogFactory.getLog(ConnectionManager.class);

	public static volatile ConcurrentMap<String, Connection> connectionMap = new ConcurrentHashMap<String, Connection>();
	
	private static ThreadLocal<Connection> currentConnectionStore = new ThreadLocal<Connection>();
	
	public static void addConnection(Connection connection){
		Connection existConn = connectionMap.remove(connection.connectionId());
		
		if(existConn != null){
			
			if(existConn.validate()){
				log.warn("Existing connection will be replaced by new comming connection, old connection: " + existConn + ", new connection: " + connection);
			}
			
			existConn.close();
		}
		
		connectionMap.put(connection.connectionId(), connection);
	}
	
	public static void removeConnection(String connectionId){
		Connection existConn = connectionMap.remove(connectionId);
		
		if(existConn != null){
			if(log.isDebugEnabled()){
				log.debug("Remove connection: " + existConn);
			}
			
			if(!existConn.isClosed()){
				existConn.close();
			}
		}
	}
	
	public static void replaceConnectionId(String connectionId, String newId){
		Connection existConn = connectionMap.remove(connectionId);
		if(existConn != null){
			existConn.setConnectionId(newId);
			
			addConnection(existConn);
		}
	}
	
	public static Set<String> getConnectionIds(){
		Set<String> connIds = new HashSet<String>();
		connIds.addAll(connectionMap.keySet());
		
		return connIds;
	}
	
	public static Connection getAnyConnection(){
		return connectionMap.isEmpty() ? null : connectionMap.values().iterator().next();
	}
	
	public static Connection getConnection(String connectionId){
		return connectionMap.get(connectionId);
	}
	
	public static boolean isValidConnection(String connectionId){
		return connectionMap.containsKey(connectionId);
	}
	
	public static void setCurrentConnection(Connection connection) {
		currentConnectionStore.set(connection);
	}
	
	public static Connection getCurrentConnection() {
		return currentConnectionStore.get();
	}
}
