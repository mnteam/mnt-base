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

package com.mnt.base.stream.client;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.comm.PacketProcessor;
import com.mnt.base.stream.comm.PacketProcessorManager;
import com.mnt.base.stream.dtd.StreamPacket;
import com.mnt.base.stream.dtd.StreamPacketDef;
import com.mnt.base.stream.netty.Connection;
import com.mnt.base.stream.server.StreamServerConfig;


/**
 * packet processor manager, start multiple threads to process XMLPacket and send back the response 
 * @author Peng Peng
 * @date 2012-5-10
 *
 */
public class ClientPacketProcessorManager extends PacketProcessorManager{
	
	private static final Log log = LogFactory.getLog(ClientPacketProcessorManager.class);
	
	private Connection connection;
	
	private static Map<Object, ClientPacketProcessorManager> processorManagerMap = new ConcurrentHashMap<Object, ClientPacketProcessorManager>();
	
	protected ClientPacketProcessorManager(){
		super(StreamServerConfig.getIntProperty("client_tcp_event_threads", 1));
	}
	
	public synchronized static ClientPacketProcessorManager getInstance(Object client){
		
		ClientPacketProcessorManager packetProcessorManager = processorManagerMap.get(client);
		
		if(packetProcessorManager == null){
			packetProcessorManager = new ClientPacketProcessorManager();
			processorManagerMap.put(client, packetProcessorManager);
		}
		
		return packetProcessorManager;
	}
	
	@Override
	protected void dispatchPacket(StreamPacket streamPacket) {
		String processorIdentifier = streamPacket.getProcessorIdentifier(); // request identifier specify the process instance
		String requestId = streamPacket.getRequestId();
		String connectionId = streamPacket.getConnectionId();
		
		if(connectionId != null){
			
			PacketProcessor packetProcessor = packetProcessorMap.get(processorIdentifier);
			
			if(packetProcessor != null){
				
				String methodIdentifier = streamPacket.getMethodIdentifier();
				
				Object resultObj = null;
				try{
					resultObj = packetProcessor.prcocessPacket(requestId, methodIdentifier, streamPacket.getPacketData());
				}catch(Exception e){
					log.error("error while process the packet: " + e.getMessage(), e);
				}
				
				if(resultObj != null){
					StreamPacket responsePacket = StreamPacket.valueOf(requestId, new StringBuilder(processorIdentifier).append(StreamPacketDef.DOT).append(methodIdentifier).toString(), resultObj);
					
					if(connection != null) {
						connection.deliver(responsePacket);
					}
				}
			}else{
				log.warn("Invalid packetIdentifier : " + processorIdentifier + ", drop the stream packet: " + streamPacket);
			}
		}else{
			log.error("Invalid logic error, it should be not go here.");
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}
}
