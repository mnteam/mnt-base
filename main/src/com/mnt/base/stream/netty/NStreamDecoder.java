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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.List;

/**
 * Decoder class that parses ByteBuffers and generates string package. Generated
 * package are then passed to the next filters.
 *
 * @author Peng Peng
 */
public class NStreamDecoder extends ByteToMessageDecoder {

	public static final AttributeKey<NStreamLightweightParser> NSTREAM_PARSER_KEY  = AttributeKey.newInstance("NSTREAM-PARSER");
    
	@Override
	protected void decode(ChannelHandlerContext chx, ByteBuf buf, List<Object> out) throws Exception {
		Attribute<NStreamLightweightParser> parserAttr = chx.channel().attr(NStreamDecoder.NSTREAM_PARSER_KEY);
		NStreamLightweightParser parser = parserAttr.get();
		if(parser == null) {
			parser = new NStreamLightweightParser();
			parserAttr.set(parser);
		}
		
		parser.read(buf);

        if (parser.areTherePackets()) {
        	out.addAll(parser.getPacketBytes());
        	parser.invalidateBufferAndClear();
        }
	}
}
