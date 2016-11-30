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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.comm.NetTraffic;
import com.mnt.base.stream.comm.PacketProcessor;
import com.mnt.base.stream.comm.PacketProcessorManager;
import com.mnt.base.stream.dtd.StreamPacket;
import com.mnt.base.stream.dtd.StreamPacketDef;
import com.mnt.base.stream.netty.Connection;


/**
 * packet processor manager, start multiple threads to process StreamPacket and send back the response 
 * @author Peng Peng
 * #date 2014-3-30
 *
 */
public class ServerPacketProcessorManager extends PacketProcessorManager{
	
	private static final Log log = LogFactory.getLog(ServerPacketProcessorManager.class);

	private static ServerPacketProcessorManager packetProcessorManager;
	
	private static boolean assureProcess = false;
	
	protected ServerPacketProcessorManager(int threadSize){
		super(threadSize);
	}
	
	public static ServerPacketProcessorManager getInstance(){
		return getInstance(false, StreamServerConfig.getTcpEventThreads());
	}
	
	public static ServerPacketProcessorManager getInstance(int threadSize){
		return getInstance(false, threadSize);
	}
	
	public static boolean isAssureProcess() {
		return assureProcess;
	}

	public static void setAssureProcess(boolean assureProcess) {
		ServerPacketProcessorManager.assureProcess = assureProcess;
	}

	public synchronized static ServerPacketProcessorManager getInstance(boolean isClient, int threadSize){
		
		if(packetProcessorManager == null){
			packetProcessorManager = new ServerPacketProcessorManager(threadSize);
		}
		
		return packetProcessorManager;
	}
	
	@Override
	protected void dispatchPacket(StreamPacket streamPacket) {
		String processorIdentifier = streamPacket.getProcessorIdentifier(); // request identifier specify the process instance
		String requestId = streamPacket.getRequestId();
		String connectionId = streamPacket.getConnectionId();
		
		if(connectionId != null){
			
			NetTraffic.log("process packet: ", "processorIdentifier: ", processorIdentifier, "requestId: ", requestId, "connectionId: ", connectionId);
			
			Connection connection = ConnectionManager.getConnection(connectionId);
			
			ConnectionManager.setCurrentConnection(connection);
			
			StreamPacket responsePacket = null;
			
			PacketProcessor packetProcessor = packetProcessorMap.get(processorIdentifier);
			
			if(packetProcessor != null){
				
				String methodIdentifier = streamPacket.getMethodIdentifier();
				
				NetTraffic.log("process packet 2: ", connection, packetProcessor);
				
				Object resultObj = packetProcessor.prcocessPacket(requestId, methodIdentifier, streamPacket.getPacketData());
				
				NetTraffic.log("process packet 3: ", resultObj);
				
				if(resultObj != null){
					responsePacket = StreamPacket.valueOf(requestId, new StringBuilder(processorIdentifier).append(StreamPacketDef.DOT).append(methodIdentifier).toString(), resultObj);
				}/* else if(assureProcess) {
					if(ConnectionManager.isValidConnection(connectionId)){
						pushPacket(streamPacket);
					}else{
						if(log.isDebugEnabled()){
							log.debug("Drop the stream packet because it is failed to process and the corresponding connection is closed, " + streamPacket);
						}
					}
				}*/
			}else{
				log.warn("Invalid packetIdentifier : " + processorIdentifier + ", drop the stream packet: " + streamPacket);
			}
			
			if(responsePacket != null){
				
				if(connection != null){
					connection.deliver(responsePacket);
				}else{
					log.warn("The connection would be disconnected, drop the response.");
				}
			}
			
			NetTraffic.log("process packet 4: ", responsePacket);
		}else{
			log.error("Invalid logic error, it should be not go here.");
		}
	}
}
