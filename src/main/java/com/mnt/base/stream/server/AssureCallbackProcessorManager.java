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

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.comm.NetTraffic;
import com.mnt.base.stream.comm.PacketProcessor;
import com.mnt.base.stream.dtd.StreamPacket;
import com.mnt.base.stream.dtd.StreamPacketDef;
import com.mnt.base.stream.netty.Connection;


/**
 * packet processor manager, start multiple threads to process XMLPacket and send back the response 
 * @author Peng Peng
 * #date 2014-3-30
 *
 */
public class AssureCallbackProcessorManager extends ServerPacketProcessorManager{
	
	private static final Log log = LogFactory.getLog(AssureCallbackProcessorManager.class);
	
	private static ServerPacketProcessorManager packetProcessorManager;
	
	protected AssureCallbackProcessorManager(int threadSize) {
		super(threadSize);
		
	}

	public static ServerPacketProcessorManager getInstance(){
		return getInstance(false, StreamServerConfig.getTcpEventThreads());
	}
	
	public static ServerPacketProcessorManager getInstance(int threadSize){
		if(packetProcessorManager == null){
			packetProcessorManager = new AssureCallbackProcessorManager(threadSize);
		}
		
		return packetProcessorManager;
	}
	
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
			String methodIdentifier = streamPacket.getMethodIdentifier();
			
			if(packetProcessor != null){
				
				NetTraffic.log("process packet 2: ", connection, packetProcessor);
				
				Object resultObj = packetProcessor.prcocessPacket(requestId, methodIdentifier, streamPacket.getPacketData());
				
				NetTraffic.log("process packet 3: ", resultObj);
				
				responsePacket = StreamPacket.valueOf(requestId, new StringBuilder(processorIdentifier).append(StreamPacketDef.DOT).append(methodIdentifier).toString(), resultObj);
				
			}else{
				log.warn("Invalid packetIdentifier : " + processorIdentifier + ", drop the stream packet: " + streamPacket);
				
				responsePacket = StreamPacket.valueOf(requestId, new StringBuilder(processorIdentifier).append(StreamPacketDef.DOT).append(methodIdentifier).toString(), new RuntimeException("no corresponding processor identifier: " + processorIdentifier));
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

	@Override
	public void run() {
		while(runningFlag){
			StreamPacket streamPacket = null;
			try {
				streamPacket = streamPacketQueueMap.get((int)Math.abs(pushAi.incrementAndGet() % maxQueueMapSize)).poll(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				log.error("Error while process the stream packet in processor queue", e);
			}
			
			if(streamPacket != null){
				
				NetTraffic.log("ready to process: ", streamPacket);
				
				try {
					dispatchPacket(streamPacket);
				} catch(Exception e) {
					
					Connection connection = ConnectionManager.getConnection(streamPacket.getConnectionId());
					
					if(connection != null && !connection.isClosed()) {
						StreamPacket errorResponse = StreamPacket.valueOf(streamPacket.getRequestId(), new StringBuilder(streamPacket.getProcessorIdentifier()).append(StreamPacketDef.DOT).append(streamPacket.getMethodIdentifier()).toString(), new RuntimeException("no corresponding processor identifier"));
						connection.deliver(errorResponse);
					}
					
					log.error("error while dispatch packet: " + streamPacket.toString(), e);
				}
			}
			
			NetTraffic.log("processed: ", streamPacket);
		}
	}
}
