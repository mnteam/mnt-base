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

import com.mnt.base.stream.comm.EventHandler;
import com.mnt.base.stream.comm.EventHandler.EventType;
import com.mnt.base.stream.comm.RequestHandler;
import com.mnt.base.stream.netty.Connection;
import com.mnt.base.stream.netty.NConnectionHandler;

import io.netty.channel.ChannelHandlerContext;

public class NServerConnectionHandler extends NConnectionHandler {
	
	private NStreamClient streamClient;
	private Connection connection = null;

    public NServerConnectionHandler(NStreamClient streamClient) {
        super();
        this.streamClient = streamClient;
    }

    @Override
    public RequestHandler createRequestHandler(Connection connection) {
    	this.connection = connection;
        return new NServerResponseHandler(connection, streamClient);
    }

	@Override
	public Connection createConnection(
			ChannelHandlerContext channelHandlerContext, String connectionId) {
		return new NClientNIOConnection(channelHandlerContext, connectionId, streamClient);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if(connection != null) {
			connection.close();
			EventHandler eh = streamClient.eventHandlerMap.get(EventType.Closed);
			
			if(eh != null) {
				eh.handleEvent();
			}
		}
		super.channelInactive(ctx);
	}
	
	
}

