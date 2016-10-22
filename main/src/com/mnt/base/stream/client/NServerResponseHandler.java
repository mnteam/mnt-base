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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.comm.EventHandler;
import com.mnt.base.stream.comm.PacketProcessorManager;
import com.mnt.base.stream.comm.RequestHandler;
import com.mnt.base.stream.comm.EventHandler.EventType;
import com.mnt.base.stream.dtd.StreamPacket;
import com.mnt.base.stream.dtd.StreamPacketDef;
import com.mnt.base.stream.netty.Connection;
import com.mnt.base.stream.netty.StreamPacketReader;
import com.mnt.base.util.CommonUtil;

public class NServerResponseHandler implements RequestHandler{
	
	private static final Log log = LogFactory.getLog(NServerResponseHandler.class);
	
	private boolean authenticated;
	private String connectionId;
	
	private PacketProcessorManager packetProcessorManager;
	private NStreamClient clientManager;

	public NServerResponseHandler(Connection connection, NStreamClient clientManager) {
		this.connectionId = connection.connectionId();
		this.clientManager = clientManager;
		
		packetProcessorManager = ClientPacketProcessorManager.getInstance(clientManager);
	}
	
	@Override
	public void process(byte[] message, StreamPacketReader reader)
			throws Exception {
        if (!authenticated) {
        	StreamPacket authPacket = new StreamPacket(message);
        	Map<String, Object> authMap = CommonUtil.uncheckedMapCast(authPacket.getPacketData());
        	if(!CommonUtil.isEmpty(authMap)) {
        		authenticated = CommonUtil.parseAsBoolean(authMap.get(StreamPacketDef.AUTH_RESULT));
        	}
        	
        	if(clientManager.authenticateFlag = authenticated) {
        		
        		EventHandler eventHandler = clientManager.eventHandlerMap.get(EventType.Authenticated);
        		if(eventHandler != null) {
        			eventHandler.handleEvent();
            	}
            	
            	clientManager.processCachedData();
        	}
        } else {
        	
            process(message);
        }
	}

	private void process(byte[] source) {
        if (source == null) {
            return;
        }

        StreamPacket packet = new StreamPacket(source);
        packet.setConnectionId(connectionId);
       
        try {
        	 processPacket(packet);
        } catch (Exception e) {
            log.error("error while deserialize packet: " + e.getMessage(), e);
        }
    }

	private void processPacket(StreamPacket packet) {
		packetProcessorManager.pushPacket(packet);
	}
}
