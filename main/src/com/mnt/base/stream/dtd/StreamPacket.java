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

package com.mnt.base.stream.dtd;


/**
 * Stream packet for TCP pack request and response.
 * 
 * @author Peng Peng
 * @date 2014-03-31
 *
 */
public class StreamPacket implements StreamPacketDef{

	private ByteArrays source;
	private String connectionId;
	private String processorIdentifier;
	private String methodIdentifier;
	private String requestId;
	private Object packetData;
	
	public StreamPacket(ByteArrays source) {
		this(source, false);
	}
	
	public StreamPacket(ByteArrays source, boolean ignoreSerialize){
		
		this.source = source;
		
		if(!ignoreSerialize){
			deSerialize();
		}
	}
	
	public StreamPacket(ByteArrays source, String connectionId,
			String processIdentifier, String methodIdentifier,
			String requestId, Object packetData) {
		super();
		this.source = source;
		this.connectionId = connectionId;
		this.processorIdentifier = processIdentifier;
		this.methodIdentifier = methodIdentifier;
		this.requestId = requestId;
		this.packetData = packetData;
	}

	public StreamPacket(byte[] source) {
		this(new ByteArrays(source));
	}

	public void deSerialize() {
		StreamPacketHelper.deSerialize(this);
	}

	public static StreamPacket valueOf(String requestId, String processorIdentifier, Object packetData){
        StreamPacket packet = StreamPacketHelper.buildStreamPacket(requestId, processorIdentifier, packetData);
		return packet;
	}

	public ByteArrays getSource() {
		return source;
	}

	public void setSource(ByteArrays source) {
		this.source = source;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	public String getProcessorIdentifier() {
		return processorIdentifier;
	}

	public void setProcessorIdentifier(String processorIdentifier) {
		this.processorIdentifier = processorIdentifier;
	}

	public String getMethodIdentifier() {
		return methodIdentifier;
	}

	public void setMethodIdentifier(String methodIdentifier) {
		this.methodIdentifier = methodIdentifier;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public Object getPacketData() {
		return packetData;
	}

	public void setPacketData(Object packetData) {
		this.packetData = packetData;
	}

	@Override
	public String toString() {
		return "StreamPacket [source=" + source
				+ ", connectionId=" + connectionId + ", processIdentifier="
				+ processorIdentifier + ", methodIdentifier=" + methodIdentifier
				+ ", requestId=" + requestId + ", packetData=" + packetData
				+ "]";
	}
}
