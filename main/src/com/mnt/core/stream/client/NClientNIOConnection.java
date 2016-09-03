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

package com.mnt.core.stream.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.core.stream.comm.EventHandler;
import com.mnt.core.stream.comm.NetTraffic;
import com.mnt.core.stream.comm.EventHandler.EventType;
import com.mnt.core.stream.dtd.StreamPacket;
import com.mnt.core.stream.netty.Connection;


public class NClientNIOConnection implements Connection {

	private static final Log log = LogFactory.getLog(NClientNIOConnection.class);


    private ChannelHandlerContext channelHandlerContext;
    private String connectionId;
    private List<StreamPacket> backupDeliverer = new ArrayList<StreamPacket>();
    private NStreamClient streamClient;

    /**
     * Flag that specifies if the connection should be considered closed. Closing a NIO connection
     * is an asynch operation so instead of waiting for the connection to be actually closed just
     * keep this flag to avoid using the connection between #close was used and the socket is actually
     * closed.
     */
    private boolean closed;


    public NClientNIOConnection(ChannelHandlerContext channelHandlerContext, String connectionId, NStreamClient streamClient) {
        this.channelHandlerContext = channelHandlerContext;
        this.connectionId = connectionId;
        this.streamClient = streamClient;
        
        closed = false;
    }

    public boolean validate() {
        if (isClosed()) {
            return false;
        }
        deliverWaitPacket();
        return !isClosed();
    }

    public byte[] getAddress() throws UnknownHostException {
        return ((InetSocketAddress) channelHandlerContext.channel().remoteAddress()).getAddress().getAddress();
    }

    public String getHostAddress() throws UnknownHostException {
        return ((InetSocketAddress) channelHandlerContext.channel().remoteAddress()).getAddress().getHostAddress();
    }

    public String getHostName() throws UnknownHostException {
        return ((InetSocketAddress) channelHandlerContext.channel().remoteAddress()).getAddress().getHostName();
    }

    public void close() {
        synchronized (this) {
            if (!isClosed()) {
            	channelHandlerContext.close();
                closed = true;
                
                EventHandler eventListener = streamClient.eventHandlerMap.get(EventType.Closed);
                if(eventListener != null) {
                	eventListener.handleEvent();
                }
            }
        }
    }

    public boolean isClosed() {
    	return closed;
    }

    public void deliver(StreamPacket packet)  {
        if (isClosed()) {
            backupDeliverer.add(packet);
        }
        else {
            boolean errorDelivering = false;
            try {
                channelHandlerContext.writeAndFlush(packet);
                NetTraffic.log("out: ", packet);
                
                // if deliver success, try to deliver the cached packets.
                while(backupDeliverer.size() > 0){
                	deliver(backupDeliverer.remove(0));
                }
            }
            catch (Exception e) {
                log.debug("NIOConnection: Error delivering packet" + "\n" + this.toString(), e);
                errorDelivering = true;
            }
            if (errorDelivering) {
                close();
                // Retry sending the packet again. Most probably if the packet is a
                // Message it will be stored offline
                backupDeliverer.add(packet);
            }
        }
    }
    
    @Override
	public void deliverWaitPacket() {
    	ByteBuf buf = channelHandlerContext.alloc().buffer(1);
    	buf.writeByte(0);
    	channelHandlerContext.writeAndFlush(buf);
	}

    @Override
	public String toString() {
        return super.toString() + " Netty connection: " + connectionId;
    }

	@Override
	public String connectionId() {
		return this.connectionId;
	}

	@Override
	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
		
	}
}
