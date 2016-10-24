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

package com.mnt.base.stream.netty;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.comm.RequestHandler;

/**
 * A ConnectionHandler is responsible for creating new sessions, destroying sessions and delivering
 * received stream package.
 *
 * @author Peng Peng
 */
@Sharable
public abstract class NConnectionHandler extends ChannelInboundHandlerAdapter {

	private static final Log Log = LogFactory.getLog(NConnectionHandler.class);

	public static final AttributeKey<RequestHandler> NSTREAM_HANDLER_KEY  = AttributeKey.newInstance("NSTREAM-HANDLER");
    public static final AttributeKey<Connection> NSTREAM_CONNECTION_KEY  = AttributeKey.newInstance("NSTREAM-CONNECTION");

    protected NConnectionHandler() {
    	// empty
    }
    
    @Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		super.handlerAdded(ctx);
		
		Connection conn = createConnection(ctx, UUID.randomUUID().toString());
        // Create a new NIOConnection for the new session
        Attribute<Connection> connAttr = ctx.channel().attr(NSTREAM_CONNECTION_KEY);
        connAttr.set(conn);
        
        Attribute<RequestHandler> handlerAttr = ctx.channel().attr(NSTREAM_HANDLER_KEY);
        handlerAttr.set(createRequestHandler(conn));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
		
		RequestHandler handler = ctx.channel().attr(NSTREAM_HANDLER_KEY).get();
		
		try {
            handler.process((byte[])message, null);
        } catch (Exception e) {
            Log.error("Closing connection due to error while processing message: " + message, e);
            Connection connection = (Connection) ctx.channel().attr(NSTREAM_CONNECTION_KEY).get();
            connection.close();
        }
	}



	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		if (cause instanceof IOException) {
            Log.debug("ConnectionHandler: ",cause);
        } else {
            Log.error(cause.getMessage(), cause);
        }
	}

    public abstract Connection createConnection(ChannelHandlerContext channelHandlerContext, String connectionId);

    public abstract RequestHandler createRequestHandler(Connection connection);
}
