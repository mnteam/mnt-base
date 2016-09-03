package com.mnt.core.stream.server;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.core.stream.comm.PacketProcessorManager;
import com.mnt.core.stream.comm.RequestHandler;
import com.mnt.core.stream.dtd.StreamPacket;
import com.mnt.core.stream.dtd.StreamPacketDef;
import com.mnt.core.stream.netty.Connection;
import com.mnt.core.stream.netty.StreamPacketReader;
import com.mnt.core.util.CommonUtil;


public class SimpleClientRequestHandler implements RequestHandler{
	
	private static final Log log = LogFactory.getLog(SimpleClientRequestHandler.class);
	
	private Connection connection;
	private boolean authenticated;
	private String connectionId;
	
	private int failedAuthCount;
	
	private PacketProcessorManager packetProcessorManager = ServerPacketProcessorManager.getInstance();
	
	private static final int AUTH_MAX_FAIL_ALLOW_COUNT = 3;
	
	public SimpleClientRequestHandler(Connection connection) {
		this.connection = connection;
		this.connectionId = connection.connectionId();
	}

	@Override
	public void process(byte[] source, StreamPacketReader parser)
			throws Exception {
        if (!authenticated) {
        	StreamPacket authPacket = new StreamPacket(source);
        	Map<String, Object> authMap = CommonUtil.uncheckedMapCast(authPacket.getPacketData());
            	
        	if(authMap != null) {
        		String fullIdf = plainPasswordAuth(authMap);
            	
            	authenticated = fullIdf != null;
            	
            	if(!authenticated){
            		failedAuthCount ++;
            	}else{
            		// we need to replace the connection id with server name
            		ConnectionManager.replaceConnectionId(connectionId, fullIdf);
            		
            		connectionId = fullIdf;
            	}
            	
            	authMap.put(StreamPacketDef.AUTH_RESULT, authenticated);
                
            	connection.deliver(StreamPacket.valueOf("0", StreamPacketDef.AUTH, authMap));
                if(failedAuthCount > AUTH_MAX_FAIL_ALLOW_COUNT){
            		// disconnect the connection
            		connection.close();
            	}
        	}
        }else{
            process(source);
        }
	}
	
	/**
	 * we only check if the servername is passed
	 * 
	 * @param authElement
	 * @return
	 */
    protected String plainPasswordAuth(Map<String, Object> authMap) {
    	String fullIdentifier = CommonUtil.castAsString(authMap.get(StreamPacketDef.AUTH_IDENTIFIER));
		String clientHost = CommonUtil.castAsString(authMap.get(StreamPacketDef.AUTH_TOKEN));
		
		if(CommonUtil.isEmpty(fullIdentifier) || CommonUtil.isEmpty(clientHost)) {
			return null;
		}
		
		return fullIdentifier;
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
