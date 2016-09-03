package com.mnt.core.stream.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.core.stream.netty.NConnectionHandler;
import com.mnt.core.stream.netty.NStreamDecoder;
import com.mnt.core.stream.netty.NStreamEncoder;
import com.mnt.core.util.CommonUtil;

public class NStreamServer {

	private static final Log Log = LogFactory.getLog(NStreamServer.class);
	
	private ServerBootstrap serverBootstrap;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	// Used to know if the sockets have been started
	private boolean isSocketStarted = false;
	
	private NStreamServer() {
		// empty
	}

	private static final NStreamServer tcpServerManager = new NStreamServer();
	
	public static NStreamServer getInstance() {
		return tcpServerManager;
	}

	private synchronized void startListener(NConnectionHandler connectionHandler) {
		if (isSocketStarted) {
			return;
		}
		isSocketStarted = true;
		startClientListener(connectionHandler);
	}

	private void startClientListener(final NConnectionHandler connectionHandler) {
		// Start clients plain socket unless it's been disabled.
		final int port = StreamServerConfig.getServerTcpPort();
		try {
			
			int ioThreads = Runtime.getRuntime().availableProcessors();
			
			bossGroup = StreamServerConfig.getBoolProperty("nstream_enable_epoll") ? new EpollEventLoopGroup(ioThreads) : new NioEventLoopGroup(ioThreads);
			workerGroup = new NioEventLoopGroup(StreamServerConfig.getTcpEventThreads());
	        
        	serverBootstrap = new ServerBootstrap();
        	serverBootstrap.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new NStreamDecoder(), new NStreamEncoder(), connectionHandler);
                 }
             })
             .option(ChannelOption.SO_BACKLOG, StreamServerConfig.getListenBackLog())
             .option(ChannelOption.SO_REUSEADDR, true)
             .childOption(ChannelOption.SO_KEEPALIVE, true);
        	
        	if(StreamServerConfig.getSocketSoLingerSeconds() > 0) {
        		serverBootstrap.option(ChannelOption.SO_LINGER, StreamServerConfig.getSocketSoLingerSeconds());
        	}
        	
        	if(StreamServerConfig.getSocketSendBufferSize() > 0) {
        		serverBootstrap.option(ChannelOption.SO_SNDBUF, StreamServerConfig.getSocketSendBufferSize());
        	}
        	
        	if(StreamServerConfig.getSocketReceiveBufferSize() > 0) {
        		serverBootstrap.option(ChannelOption.SO_RCVBUF, StreamServerConfig.getSocketReceiveBufferSize());
        	}

            // Listen on a specific network interface if it has been set.
			InetAddress bindInterface = null;

			String interfaceName = StreamServerConfig.getTcpBindInterfaceName();
			if (!CommonUtil.isEmpty(interfaceName)) {
				bindInterface = InetAddress.getByName(interfaceName);
			}
			
            // Bind and start to accept incoming connections.
            ChannelFuture future = serverBootstrap.bind(new InetSocketAddress(bindInterface, port)).sync();
            future.addListener(new GenericFutureListener<Future<Void>>(){

				@Override
				public void operationComplete(Future<Void> future)
						throws Exception {
					Log.info("Bound the port: " + port);
				}
            });
            
			
			Log.info("The TCP Server is running.");
		} catch (Exception e) {
			Log.error("Error when starting TCP Server on port " + port, e);
		}
	}

	private void stopListener() {
		if (serverBootstrap != null) {
			workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
			
			Log.info("TCP server is stopped.");
		}else{
			Log.warn("Error while stop TCP server, the server would be failed to startup.");
		}
	}

	public void start(NConnectionHandler connectionHandler) {
		Log.info("Prepare to startup TCP server...");
		startListener(connectionHandler);
	}

	public void stop() {
		Log.info("Try to stop TCP server...");
		stopListener();
	}
}
