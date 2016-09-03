package com.mnt.core.stream.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.core.stream.comm.EventHandler;
import com.mnt.core.stream.comm.PacketProcessor;
import com.mnt.core.stream.comm.EventHandler.EventType;
import com.mnt.core.stream.dtd.StreamPacket;
import com.mnt.core.stream.dtd.StreamPacketDef;
import com.mnt.core.stream.netty.Connection;
import com.mnt.core.stream.netty.NConnectionHandler;
import com.mnt.core.stream.netty.NStreamDecoder;
import com.mnt.core.stream.netty.NStreamEncoder;
import com.mnt.core.stream.server.StreamServerConfig;

public class NStreamClient implements Runnable {

	protected final Log log = LogFactory.getLog(getClass());

	protected String serverIp;
	protected int port;
	protected Thread thread;

	String identifier;
	protected String authToken;

	protected volatile boolean reconnectFlag; // if the client need to reconnect to server.
	protected volatile boolean runningFlag; // if the client need to shutdown.
	protected volatile boolean connectedFlag; // if the client connected to server.
	protected volatile boolean authenticateFlag; // if the client is authenticated.

	protected Connection connection;
	
	protected Map<EventType, EventHandler> eventHandlerMap = new HashMap<EventType, EventHandler>();
	
	// store the object which the connection is lost
	protected BlockingQueue<PacketData> cachedRecordsQueue = new LinkedBlockingQueue<PacketData>();
	
	private EventLoopGroup workerGroup;

	protected int maxFailedCacheSize = 10000;
	protected static final int CONNECT_TIMEOUT = 3; // mill seconds
	
	public NStreamClient(String serverIp, int port){
		this(serverIp, port, 0, false);
	}
	
	public NStreamClient(String serverIp, int port, int maxFailedCacheSize, boolean disableAutoReconnect) {
		this.serverIp = serverIp;
		this.port = port;
		runningFlag = true;
		
		if(!disableAutoReconnect) {
			thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}
		
		if(maxFailedCacheSize > 0){
			this.maxFailedCacheSize = maxFailedCacheSize;
		}
	}
	
	public void addPacketProcessor(PacketProcessor packetProcessor){
		ClientPacketProcessorManager.getInstance(this).addProcessor(packetProcessor);
	}
	
	public void increasePacketProcessorThread(int threadSize) {
		ClientPacketProcessorManager.getInstance(this).increaseProcessorThreads(threadSize);
	}

	public boolean connect(String identifier, String authToken) {

		this.identifier = identifier;
		this.authToken = authToken;

		if (!connectedFlag) {
			try {
				
				if(connection != null){
					connection.close();
				}
		        
				int threads = StreamServerConfig.getIntProperty("client_tcp_event_threads");
		        workerGroup = threads > 0 ? new NioEventLoopGroup(threads) : new NioEventLoopGroup(threads);
		        
	            Bootstrap clientBootstrap = new Bootstrap(); 
	            clientBootstrap.group(workerGroup);
	            clientBootstrap.channel(NioSocketChannel.class);
	            clientBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
	            clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
	                @Override
	                public void initChannel(SocketChannel ch) throws Exception {
	                    ch.pipeline().addLast(new NStreamDecoder(), new NStreamEncoder(), new NServerConnectionHandler(NStreamClient.this));
	                    
	                    ChannelHandlerContext chx = ch.pipeline().lastContext();
	                    //connection = new NClientNIOConnection(chx, UUID.randomUUID().toString(), NStreamClient.this);
						connection = chx.channel().attr(NConnectionHandler.NSTREAM_CONNECTION_KEY).get();
	                    ClientPacketProcessorManager.getInstance(this).setConnection(connection);
						//chx.attr(NConnectionHandler.NSTREAM_HANDLER_KEY).set(new NServerResponseHandler(connection, NStreamClient.this));
	                }
	            });

	            clientBootstrap.connect(serverIp, port).await(CONNECT_TIMEOUT, TimeUnit.SECONDS);
	            
	            connectedFlag = (connection != null);
				
				if(connectedFlag) {
					Map<String, Object> authMap = new HashMap<String, Object>();
					authMap.put(StreamPacketDef.AUTH_IDENTIFIER, NStreamClient.this.identifier);
					authMap.put(StreamPacketDef.AUTH_TOKEN, NStreamClient.this.authToken);
					
					StreamPacket authPacket = StreamPacket.valueOf("0", StreamPacketDef.AUTH, authMap);
					
					connection.deliver(authPacket);
				}
				
			} catch (Exception e) {

				startReconnect();
				log.error("Error while create socket connection to monitor server: " + serverIp + ", port: " + port, e);
			}
		}

		return authenticateFlag;
	}

	void processCachedData() {
		//process the previously unsend multiLogRecords object
		int size = cachedRecordsQueue.size();
		for(int i = 0; i < size; i++){
			
			PacketData cachedPacketData = null;
			try {
				cachedPacketData = cachedRecordsQueue.poll(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				log.error("fail to retrieve the multiLogRecords from cached queue.", e);
				throw new RuntimeException("fail to retrieve the multiLogRecords from cached queue.", e);
			}
			
			if(cachedPacketData != null){
				this.deliver(cachedPacketData.processorIdentifier, cachedPacketData.packetData);
			}else{
				break;
			}
		}
	}

	private boolean connect() {
		return this.connect(identifier, authToken);
	}

	@Override
	public void run() {
		while (runningFlag) {
			
			try {
				Thread.sleep(1000 * 30); // try to reconnect every 30 sec
			} catch (InterruptedException e) {
				log.error("Error while sleeping, the client would be shutdown now?", e);
				break;
			}
			
			if (authenticateFlag) {
				try{
					
					if(connection.isClosed()){
						startReconnect();
					} else {
						// try ping server in ervery 30 sec
						connection.deliverWaitPacket();
					}
					
				}catch(Exception e){
					startReconnect();
				}
			} else if (reconnectFlag) {
				connect();
			}
		}
	}

	public boolean deliver(String processorIdentifier, Object packetData) {
		if (authenticateFlag && !connection.isClosed()) {
			try {
				
				StreamPacket packet = StreamPacket.valueOf(UUID.randomUUID().toString(), processorIdentifier, packetData);
				
				connection.deliver(packet);
				return true;
			} catch (Exception e) {
				log.error("fail to write object to server.");
				startReconnect();
				
				putFailedDataToCache(new PacketData(processorIdentifier, packetData));
			}
		} else {
			putFailedDataToCache(new PacketData(processorIdentifier, packetData));
		}

		return false;
	}
	
	public boolean deliver(StreamPacket packet) {
		if (authenticateFlag && !connection.isClosed()) {
			try {
				connection.deliver(packet);
				return true;
			} catch (Exception e) {
				log.error("fail to write object to server.");
				startReconnect();
			}
		}

		return false;
	}

	private void putFailedDataToCache(PacketData packetData){
		try {
			while(cachedRecordsQueue.size() > maxFailedCacheSize){
				log.warn("Drop un-dispatched multiLogRecords: " + cachedRecordsQueue.poll(1, TimeUnit.SECONDS));
			}
			
			cachedRecordsQueue.put(packetData);
		} catch (InterruptedException e1) {
			log.error("fail to put the multi log record to cache.", e1);
			throw new RuntimeException("fail to put the multiLogRecords to cached queue.", e1); // the server would be shutdown..
		}
	}
	
	public void disconnect() {
		runningFlag = false;
		connection.close();
		workerGroup.shutdownGracefully();
	}
	
	private void startReconnect() {
		reconnectFlag = true;
		connectedFlag = false;
		authenticateFlag = false;
	}
	
	public void addEventHandler(EventHandler eventListener) {
		this.eventHandlerMap.put(eventListener.eventType(), eventListener);
	}
	
	public void removeEventHandler(EventType eventType) {
		this.eventHandlerMap.remove(eventType);
	}

	class PacketData{
		
		public PacketData(String processorIdentifier,
				Object packetData) {
			super();
			this.processorIdentifier = processorIdentifier;
			this.packetData = packetData;
		}

		String processorIdentifier;
		Object packetData;
		
		void clear(){
			processorIdentifier = null;
			packetData = null;
		}
	}
}
