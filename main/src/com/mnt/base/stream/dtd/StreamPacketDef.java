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
 * <pre>
 * providing two way to pack the stream packet:
 * 1. if the transferring object is byte array 
 * build two part as stream packet:
 * 
 * stream start 32bit stream packet len                   reserved flags  compress flag        packet type 0      16 bit stream head len   head block             object block          validation code       stream end
 * [0000 0001] [0000 0000 0000 0000 0000 0000 0000 0000]  [0000 00        0                    1]                 [0000 0000 0000 0000]    [0000 0000 ... 0000]   [0000 0000 ... 0000] [0000 0000 0000 0000] [0000 0000]   
 * 
 * 
 * 2. if the transferring object is object
 * build only one part
 * 
 * stream start 32bit stream packet len                    reserved flags  compress flag        packet type 1      object block         validation code       stream end
 * [0000 0001]  [0000 0000 0000 0000 0000 0000 0000 0000]  [0000 00        0                    0]                 [0000 0000 ... 0000] [0000 0000 0000 0000] [0000 0000]
 * 
 * which means the packet max length should be Integer.MAX_VALUE(2^30) byte. for fisrt bit of the stream packet len mark the packet type
 * 
 * </pre>
 * @author Peng Peng
 * @date 2014-03-31
 *
 */
public interface StreamPacketDef {
	// properties
	String REQUEST_ID 				= "requestId";
	String IDENTIFIER 				= "identifier";
	String DATA						= "data";
	
	String AUTH_RESULT 				= "result";
	String AUTH_IDENTIFIER			= "authIdentifier";
	String AUTH_TOKEN				= "token";
	
	byte BYTE_ARRAY_PACKET_FLAG     = 0x01;
	byte COMPRESS_FLAG 				= 0x02;
	String IDENTIFIER_SPLITTER_REG	= "\\.";
	String DOT 						= ".";
	
	byte BYTE_STREAM_PACKET_START 	= 0x01;
	byte BYTE_STREAM_PACKET_END		= 0x00;
	
	int PACKET_LEN_BYTE_SIZE		= 4;
	int PACKET_VC_BYTE_SIZE			= 2;
	String AUTH 					= "__auth.plain";
}