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

package com.mnt.core.stream.server;

import com.mnt.core.util.BaseConfiguration;

/**
 * 
 * @author	Peng Peng
 * @date 	2012-03-22
 * @updated	2015-07-15
 *
 * Providing all configuration items for tcp server.
 */
public class StreamServerConfig extends BaseConfiguration {
	
	private static int serverTcpPort				= 0;
	private static String tcpBindInterfaceName		= null;
	private static int tcpEventThreads				= 1;
	private static long eventThreadAliveTime		= 60; // unit: sec
	private static int listenBackLog				= 50;
	private static int socketReceiveBufferSize		= -1;
	private static int socketSendBufferSize			= -1;
	private static int socketSoLingerSeconds		= -1;
	private static int tcpMaxIdleTime				= 6 * 60 * 1000;
	private static boolean disablePacketCacheQueue	= false;
	
	private final static StreamServerConfig instance = new StreamServerConfig();
	static {
		setupDefaultConfItems();
	}
	
	protected StreamServerConfig(){
		super();
	}
	
	protected StreamServerConfig(String confPath) {
		super(confPath);
		setupDefaultConfItems();
	}
	
	public static StreamServerConfig getInstance() {
		return instance;
	}
	
	private static void setupDefaultConfItems() {
		serverTcpPort       		= getIntProperty(ItemKeyDef.K_SERVER_TCP_PORT, 5050);
		eventThreadAliveTime 		= getIntProperty(ItemKeyDef.K_EVENT_THREAD_ALIVE_TIME, 60);
		tcpEventThreads      		= getIntProperty(ItemKeyDef.K_TCP_EVENT_THREADS, 1);
		tcpBindInterfaceName 		= getProperty(ItemKeyDef.K_TCP_BIND_INTERFACE_NAME);
		listenBackLog 				= getIntProperty(ItemKeyDef.K_LISTEN_BACK_LOG, 50);
		socketReceiveBufferSize 	= getIntProperty(ItemKeyDef.K_SOCKET_RECEIVE_BUFFER_SIZE, -1);
		socketSendBufferSize 		= getIntProperty(ItemKeyDef.K_SOCKET_SEND_BUFFER_SIZE, -1);
		socketSoLingerSeconds 		= getIntProperty(ItemKeyDef.K_SOCKET_SO_LINGER_SECONDS, -1);
		tcpMaxIdleTime				= getIntProperty(ItemKeyDef.K_TCP_MAX_IDLE_TIME, 6 * 60 * 1000);
		disablePacketCacheQueue 	= getBoolProperty(ItemKeyDef.K_DISABLE_PACKET_CACHE_QUEUE);
	}
	
	public static int getServerTcpPort() {
		return serverTcpPort;
	}
	
	public static String getTcpBindInterfaceName() {
		return tcpBindInterfaceName;
	}
	
	public static int getTcpEventThreads() {
		return tcpEventThreads;
	}

	public static long getEventThreadAliveTime() {
		return eventThreadAliveTime;
	}

	public static int getListenBackLog() {
		return listenBackLog;
	}

	public static int getSocketReceiveBufferSize() {
		return socketReceiveBufferSize;
	}

	public static int getSocketSendBufferSize() {
		return socketSendBufferSize;
	}

	public static int getSocketSoLingerSeconds() {
		return socketSoLingerSeconds;
	}

	public static int getTcpMaxIdleTime() {
		return tcpMaxIdleTime;
	}
	
	public static boolean disablePacketCacheQueue() {
		return disablePacketCacheQueue;
	}
	
	private interface ItemKeyDef {
		String K_SERVER_TCP_PORT      		= "server_tcp_port";
		String K_TCP_BIND_INTERFACE_NAME 	= "tcp_bind_interface_name";
		String K_EVENT_THREAD_ALIVE_TIME 	= "event_thread_alive_time";
		String K_TCP_EVENT_THREADS 			= "tcp_event_threads";
		String K_LISTEN_BACK_LOG 			= "listen_back_log";
		String K_SOCKET_RECEIVE_BUFFER_SIZE = "socket_receive_buffer_size";
		String K_SOCKET_SEND_BUFFER_SIZE 	= "socket_send_buffer_size";
		String K_SOCKET_SO_LINGER_SECONDS 	= "socket_so_linger_seconds";
		String K_TCP_MAX_IDLE_TIME 			= "tcp_max_idle_time";
		String K_DISABLE_PACKET_CACHE_QUEUE = "disable_packet_cache_queue";
	}
}
