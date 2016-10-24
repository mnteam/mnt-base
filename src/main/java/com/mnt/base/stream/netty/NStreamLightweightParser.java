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

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mnt.base.stream.comm.BytesUtil;
import com.mnt.base.stream.dtd.StreamPacketDef;

/**
 * This is a Light-Weight stream Parser.
 * It read data from a channel and collect data until data are available in
 * the channel.
 * When a message is complete you can retrieve messages invoking the method
 * getMsgs() and you can invoke the method areThereMsgs() to know if at least
 * an message is presents.
 *
 * @author Peng Peng
 */
public class NStreamLightweightParser {
	
	//private static final Log Log = LogFactory.getLog(XMLLightweightParser.class);
	
    private static int maxBufferSize;

    // Buffer with all data retrieved
    protected ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private List<byte[]> cachedPackets = new ArrayList<byte[]>();
    
    private int curIndex;
    private int curSize;
    
    private byte[] packetLenBytes = new byte[StreamPacketDef.PACKET_LEN_BYTE_SIZE];
    private byte[] vcBytes = new byte[StreamPacketDef.PACKET_VC_BYTE_SIZE];
    
    private int status = 0;
    
    private static final int STATUS_WAIT 		= 0;
    private static final int STATU_READ_LEN		= 1;
    private static final int STATUS_READ_DATA 	= 2;
    private static final int STATUS_READ_VD 	= 3;
    
    static {
        // Set default max buffer size to 100MB. If limit is reached then close connection
        maxBufferSize = 104857600;
        // Listen for changes to this property
    }

    public NStreamLightweightParser() {
    	// empty
    }

    /*
    * true if the parser has found some complete xml message.
    */
    public boolean areTherePackets() {
        return (cachedPackets.size() > 0);
    }

    /*
    * @return an array with all messages found
    */
    public List<byte[]> getPacketBytes() {
        return cachedPackets;
    }

    /*
    * Method use to re-initialize the buffer
    */
    protected void invalidateBufferAndClear() {
    	 cachedPackets.clear();
    }


    /*
    * Method that add a message to the list and reinit parser.
    */
    protected void foundMsgEnd() {
        // Add message to the complete message list
    	buffer.reset();
    	curIndex = -1;
    	curSize = 0;
    	status = STATUS_WAIT;
    }

    /*
    * Main reading method
    */
    public void read(ByteBuf byteBuffer) throws Exception {
        
        // Check that the buffer is not bigger than 100 Megabyte. For security reasons
        // we will abort parsing when 100 MB of queued byte was found.
        if (buffer.size() > maxBufferSize) {
            throw new Exception("Stopped parsing never ending stream");
        }
        
        int len = byteBuffer.readableBytes();
        
        if(len > 0) {
        	byte[] bytes = new byte[len];
            byteBuffer.readBytes(bytes, 0, len);
            readBytes(bytes, 0);
        }
    }

	private void readBytes(byte[] bytes, int position) throws Exception {
		
		if(status == STATUS_READ_DATA) {
			int delta = curSize - curIndex;
			
			int remaining = bytes.length - position;
			
			if(remaining < delta) {
				buffer.write(bytes, position, remaining);
				curIndex += remaining;
			} else {
				buffer.write(bytes, position, delta);
				curIndex = curSize;
				
				position += delta;
				status = STATUS_READ_VD;
				curIndex = 0;
				curSize = StreamPacketDef.PACKET_VC_BYTE_SIZE;
				
				if(position < bytes.length) {
					readBytes(bytes, position);
				}
			}
		} else {
			switch(status) {
				case STATUS_WAIT : {
					for(; position < bytes.length; position++) {
						if(bytes[position] == StreamPacketDef.BYTE_STREAM_PACKET_START) {
							status = STATU_READ_LEN;
							curIndex = 0;
							curSize = StreamPacketDef.PACKET_LEN_BYTE_SIZE;
							position ++;
							break;
						} 
					}
					
					if(position == bytes.length) {
						break;
					}
				}
				case STATU_READ_LEN : {
					
					while(position < bytes.length) {
						if(curIndex == curSize) {
							break;
						}
						
						packetLenBytes[curIndex++] = bytes[position++];
					}
					
					if(curIndex == curSize) {
						curIndex = 0;
						curSize = BytesUtil.bytesToInt(packetLenBytes);
						status = STATUS_READ_DATA;
					}
					
					if(position < bytes.length) {
						readBytes(bytes, position);
					}
					break;
				}
				case STATUS_READ_VD : {
					while(position < bytes.length) {
						if(curIndex == curSize) {
							break;
						}
						
						vcBytes[curIndex++] = bytes[position++];
					}
					
					if(curIndex == curSize) {
						curIndex = 0;
						byte[] source = buffer.toByteArray();
						if(BytesUtil.equals(BytesUtil.genSign(source), vcBytes)) {
							cachedPackets.add(source);
							foundMsgEnd();
						} else {
							throw new Exception("error while check the vc:" + Arrays.toString(vcBytes) + ", get sign value: " + BytesUtil.genSign(source));
						}
						
						status = STATUS_WAIT;
					}
					
					if(position < bytes.length) {
						readBytes(bytes, position);
					}
				}
			}
		}
	}
}
