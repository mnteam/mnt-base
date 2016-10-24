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

import java.util.HashMap;
import java.util.Map;

import com.mnt.base.stream.comm.BytesUtil;
import com.mnt.base.stream.comm.CompressHelper;
import com.mnt.base.stream.comm.StreamUtils;
import com.mnt.base.stream.dtd.ByteArrays.ByteArray;
import com.mnt.base.util.CommonUtil;

/**
 * stream packet helper class.
 * @author Peng Peng
 * #date 2014-3-30
 *
 *
 */
public abstract class StreamPacketHelper {
	
	public static StreamPacket buildStreamPacket(String requestId,
			String processorIdentifier, Object packetData) {
		
		ByteArrays source = new ByteArrays();
		Map<String, Object> packetInfoMap = new HashMap<String, Object>();
		packetInfoMap.put(StreamPacketDef.REQUEST_ID, requestId);
		packetInfoMap.put(StreamPacketDef.IDENTIFIER, processorIdentifier);
		boolean isArrayPacket = packetData instanceof byte[];
		byte packetDescByte = isArrayPacket ? StreamPacketDef.BYTE_ARRAY_PACKET_FLAG : 0;
		
		if(isArrayPacket) {// while array packet data, no compress support
			byte[] headBytes = StreamUtils.getSerializeHelper().getInstance().serialize(packetInfoMap);
			Number headBlockLen = headBytes.length;
			if(headBlockLen.intValue() > Short.MAX_VALUE) {
				throw new RuntimeException("stream packet head block over the max size: " + Short.MAX_VALUE +", actually size: " + headBlockLen);
			}
			
			byte[] headLenBytes = BytesUtil.shortToBytes(headBlockLen.shortValue());
			byte[] packetDataArr = ((byte[])packetData);
			
			source.add(headLenBytes);
			source.add(headBytes);
			
			if(StreamUtils.isEnableCompress() && packetDataArr.length > StreamUtils.getMinCompressSize()) {
				CompressHelper ch = StreamUtils.getCompressHelper().getInstance();
				
				packetDescByte |= StreamPacketDef.COMPRESS_FLAG;
				
				if(ch.useByteArray()) {
					ByteArray ba = source.new ByteArray(packetDataArr);
					ch.compress(ba);
					source.add(ba);
				} else {
					packetDataArr = ch.compress(packetDataArr);
					source.add(packetDataArr);
				}
			} else {
				source.add(packetDataArr);
			}
		} else {
			packetInfoMap.put(StreamPacketDef.DATA, packetData);
			byte[] packetDataArr = StreamUtils.getSerializeHelper().serialize(packetInfoMap);
			if(StreamUtils.isEnableCompress() && packetDataArr.length > StreamUtils.getMinCompressSize()) {
				
				CompressHelper ch = StreamUtils.getCompressHelper().getInstance();
				
				packetDescByte |= StreamPacketDef.COMPRESS_FLAG;
				
				if(ch.useByteArray()) {
					ByteArray ba = source.new ByteArray(packetDataArr);
					ch.compress(ba);
					source.add(ba);
				} else {
					packetDataArr = ch.compress(packetDataArr);
					source.add(packetDataArr);
				}
			} else {
				source.add(packetDataArr);
			}
		}
		
		// add packet desc byte
		source.insert(0, packetDescByte);
		
		return new StreamPacket(source, true);
	}

	public static void deSerialize(StreamPacket streamPacket) {
		byte[] source = streamPacket.getSource().toByteArray();
		
		
		Map<String, Object> headInfoMap = null;
		
		byte packetDescByte = source[0];
		boolean isArrayPacket = (packetDescByte & StreamPacketDef.BYTE_ARRAY_PACKET_FLAG) > 0;
		if(isArrayPacket) {
			byte[] headLenBytes = new byte[2];
			headLenBytes[0] = source[1];
			headLenBytes[1] = source[2];
			
			short headLen = BytesUtil.bytesToShort(headLenBytes);
			byte[] headBytes = new byte[headLen];
			System.arraycopy(source, 3, headBytes, 0, headLen);
			
			headInfoMap = CommonUtil.uncheckedMapCast(StreamUtils.getSerializeHelper().getInstance().deserialize(headBytes));
			
			int preInfoLen =  1 + 2 + headLen;
			int bodyArrLen = source.length - preInfoLen;
			byte[] packetDataArr = new byte[bodyArrLen];
			if(bodyArrLen > 0) {
				System.arraycopy(source, preInfoLen, packetDataArr, 0, bodyArrLen);
			}
			
			boolean compressFlag = (packetDescByte & StreamPacketDef.COMPRESS_FLAG) > 0;
			if(compressFlag) {
				packetDataArr = StreamUtils.getCompressHelper().getInstance().decompress(packetDataArr);
			}
			
			streamPacket.setPacketData(packetDataArr);
		} else {
			byte[] packetDataArr = new byte[source.length - 1];
			System.arraycopy(source, 1, packetDataArr, 0, source.length - 1);
			
			boolean compressFlag = (packetDescByte & StreamPacketDef.COMPRESS_FLAG) > 0;
			if(compressFlag) {
				packetDataArr = StreamUtils.getCompressHelper().getInstance().decompress(packetDataArr);
			}
			
			headInfoMap = CommonUtil.uncheckedMapCast(StreamUtils.getSerializeHelper().deserialize(packetDataArr));
		}
		
		if(CommonUtil.isEmpty(headInfoMap)) {
			throw new RuntimeException("invalid stream packet with empty head info map");
		}
		
		streamPacket.setRequestId(CommonUtil.castAsString(headInfoMap.get(StreamPacketDef.REQUEST_ID)));
		String processorIdentifier = CommonUtil.castAsString(headInfoMap.get(StreamPacketDef.IDENTIFIER));
		if(!CommonUtil.isEmpty(processorIdentifier)) {
			String[] identifiers = processorIdentifier.split(StreamPacketDef.IDENTIFIER_SPLITTER_REG, 2);
			streamPacket.setProcessorIdentifier(identifiers[0]);
			if(identifiers.length == 2) {
				streamPacket.setMethodIdentifier(identifiers[1]);
			}
		}
		
		if(!isArrayPacket) {
			streamPacket.setPacketData(headInfoMap.get(StreamPacketDef.DATA));
		}
	}
}
