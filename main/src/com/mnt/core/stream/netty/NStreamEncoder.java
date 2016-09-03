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

package com.mnt.core.stream.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;

import com.mnt.core.stream.comm.BytesUtil;
import com.mnt.core.stream.dtd.ByteArrays;
import com.mnt.core.stream.dtd.StreamPacket;
import com.mnt.core.stream.dtd.StreamPacketDef;
import com.mnt.core.stream.dtd.ByteArrays.ByteArray;

/**
 * Encoder that does nothing. We are already writing ByteBuffers so there is no need
 * to encode them.<p>
 *
 * This class exists as a counterpart of {@link NStreamDecoder}. Unlike that class this class does nothing.
 *
 * @author Peng Peng
 */
public class NStreamEncoder extends MessageToByteEncoder<StreamPacket> {

	@Override
	protected void encode(ChannelHandlerContext ctx, StreamPacket packet, ByteBuf out) throws Exception {

		
		Attribute<NStreamLightweightParser> parserAttr = ctx.channel().attr(NStreamDecoder.NSTREAM_PARSER_KEY);
		NStreamLightweightParser parser = parserAttr.get();
		if(parser == null) {
			parser = new NStreamLightweightParser();
			parserAttr.set(parser);
		}
		
		ByteArrays source = packet.getSource();
		
		out.writeByte(StreamPacketDef.BYTE_STREAM_PACKET_START);
		out.writeBytes(BytesUtil.intToBytes(source.getTotalLength()));
		
		for(ByteArray ba : source.getByteArrays()) {
			out.writeBytes(ba.getBytes(), ba.getPosition(), ba.getLength());
		}
		
		out.writeBytes(BytesUtil.genSign(source));
		out.writeByte(StreamPacketDef.BYTE_STREAM_PACKET_END);
	}
}
