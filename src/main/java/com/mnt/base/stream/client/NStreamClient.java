package com.mnt.base.stream.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.comm.EventHandler;
import com.mnt.base.stream.comm.EventHandler.EventType;
import com.mnt.base.stream.comm.PacketProcessor;
import com.mnt.base.stream.dtd.StreamPacket;
import com.mnt.base.stream.dtd.StreamPacketDef;
import com.mnt.base.stream.netty.Connection;
import com.mnt.base.stream.netty.NConnectionHandler;
import com.mnt.base.stream.netty.NStreamDecoder;
import com.mnt.base.stream.netty.NStreamEncoder;
import com.mnt.base.util.ClientConfiguration;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NStreamClient implements Runnable {

	protected final Log log = LogFactory.getLog(getClass());

	protected String serverHost;
	protected int serverPort;
	
	protected Thread statusTrackThread;

	protected String identifier;
	protected String authToken;

	protected volatile boolean reconnectFlag; 		// if the client need to reconnect to server.
	protected volatile boolean runningFlag; 		// if the client need to shutdown.
	protected volatile boolean connectedFlag; 		// if the client connected to server.
	protected volatile boolean authenticateFlag; 	// if the client is authenticated.

	protected Connection connection;
	
	private EventLoopGroup workerGroup;
	
	protected Map<EventType, EventHandler> eventHandlerMap = new HashMap<EventType, EventHandler>();
	
	// store the object which the connection is lost
	protected BlockingQueue<PacketData> cachedRecordsQueue = new LinkedBlockingQueue<PacketData>();
	
	protected int maxFailedCacheSize = 10000;
	
	protected static final int CONNECT_TIMEOUT = 3; // mill seconds
	
	protected ClientPacketProcessorManager clientPacketProcessorManager = ClientPacketProcessorManager.getInstance(this);
	
	public NStreamClient(String serverHost, int serverPort) {
		this(serverHost, serverPort, 0, false);
	}
	
	public NStreamClient(String serverHost, int serverPort, int maxFailedCacheSize, boolean disableAutoReconnect) {
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		runningFlag = true;
		
		if(!disableAutoReconnect) {
			statusTrackThread = new Thread(this, "NStreamClient Status Track Thread");
			statusTrackThread.setDaemon(true);
			statusTrackThread.start();
		}
		
		if(maxFailedCacheSize > 0) {
			this.maxFailedCacheSize = maxFailedCacheSize;
		}
	}
	
	public void addPacketProcessor(PacketProcessor packetProcessor) {
		clientPacketProcessorManager.addProcessor(packetProcessor);
	}
	
	public void increasePacketProcessorThread(int threadSize) {
		clientPacketProcessorManager.increaseProcessorThreads(threadSize);
	}
	
	protected NServerConnectionHandler getNServerConnectionHandler(NStreamClient streamClient) {
		return new NServerConnectionHandler(NStreamClient.this);
	}
	
	protected void authenticate() {
		Map<String, Object> authMap = new HashMap<String, Object>();
		authMap.put(StreamPacketDef.AUTH_IDENTIFIER, NStreamClient.this.identifier);
		authMap.put(StreamPacketDef.AUTH_TOKEN, NStreamClient.this.authToken);
		
		StreamPacket authPacket = StreamPacket.valueOf("0", StreamPacketDef.AUTH, authMap);
		
		connection.deliver(authPacket);
	}

	public boolean connect(String identifier, String authToken) {

		this.identifier = identifier;
		this.authToken = authToken;

		if (!connectedFlag) {
			try {
				
				if(connection != null) {
					connection.close();
				}
		        
				int threads = ClientConfiguration.getIntProperty("client_tcp_event_threads");
		        workerGroup = threads > 0 ? new NioEventLoopGroup(threads) : new NioEventLoopGroup(1);
		        
	            Bootstrap clientBootstrap = new Bootstrap(); 
	            clientBootstrap.group(workerGroup);
	            clientBootstrap.channel(NioSocketChannel.class);
	            clientBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
	            clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
	                @Override
	                public void initChannel(SocketChannel ch) throws Exception {
	                    ch.pipeline().addLast(new NStreamDecoder(), new NStreamEncoder(), getNServerConnectionHandler(NStreamClient.this));
	                    
	                    ChannelHandlerContext chx = ch.pipeline().lastContext();
						connection = chx.channel().attr(NConnectionHandler.NSTREAM_CONNECTION_KEY).get();
						clientPacketProcessorManager.setConnection(connection);
	                }
	            });

	            clientBootstrap.connect(serverHost, serverPort).await(CONNECT_TIMEOUT, TimeUnit.SECONDS);
	            
	            connectedFlag = (connection != null);
				
				if(connectedFlag) {
					authenticate();
				}
				
			} catch (Exception e) {

				startReconnect();
				log.error("Error while create socket connection to monitor server: " + serverHost + ", port: " + serverPort, e);
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

	protected boolean connect() {
		return this.connect(identifier, authToken);
	}

	// default try to reconnect every 30 sec
	protected long checkIntervalMs() {
		return 1000 * 30;
	}
	
	@Override
	public void run() {
		while (runningFlag) {
			
			try {
				Thread.sleep(checkIntervalMs()); 
			} catch (InterruptedException e) {
				log.error("Error while sleeping, the client would be shutdown now?", e);
				break;
			}
			
			if (reconnectFlag) {
				startReconnect();
				connect();
				reconnectFlag = false;
			} else if (authenticateFlag) {
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
			} else {
				startReconnect();
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
	
	protected void startReconnect() {
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
