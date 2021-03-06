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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.comm.NetTraffic;
import com.mnt.base.stream.dtd.StreamPacket;
import com.mnt.base.stream.netty.Connection;


/**
 * Implementation of {@link Connection} interface specific for NIO connections when using
 * the Netty framework.<p>

 * @author Peng Peng
 */
public abstract class NAbstractNIOConnection implements Connection {

	private static final Log log = LogFactory.getLog(NAbstractNIOConnection.class);

    /**
     * The utf-8 charset for decoding and encoding stream packet streams.
     */
    protected ChannelHandlerContext channelHandlerContext;
    protected String connectionId;
    protected List<StreamPacket> backupDeliverer = new ArrayList<StreamPacket>();

    /**
     * Flag that specifies if the connection should be considered closed. Closing a NIO connection
     * is an asynch operation so instead of waiting for the connection to be actually closed just
     * keep this flag to avoid using the connection between #close was used and the socket is actually
     * closed.
     */
    private boolean closed;


    public NAbstractNIOConnection(ChannelHandlerContext channelHandlerContext, String connectionId) {
        this.channelHandlerContext = channelHandlerContext;
        this.connectionId = connectionId;
        
        closed = false;
        
        ConnectionManager.addConnection(this);
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
                closing();
                
                ConnectionManager.removeConnection(this.connectionId);
            }
        }
    }
    
    protected abstract void closing();

    public boolean isClosed() {
    	return closed;
    }

    public void deliver(StreamPacket packet) {
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

	public String getConnectionId() {
		return connectionId;
	}
}