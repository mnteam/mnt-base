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

package com.mnt.base.stream.comm;

import com.mnt.base.stream.dtd.ByteArrays;

/**
 * 
 * @author Peng Peng
 *
 */
public class BytesUtil {

	public static byte[] shortToBytes(short s) {
		return new byte[]{(byte)(s), (byte)(s >> 8)};
	}
	
	public static short bytesToShort(byte[] bs) {
		return (short)((bs[0] & 0xff) | (bs[1] << 8));
	}
	
	public static byte[] intToBytes(int i) {
		return new byte[]{(byte)(i), (byte)(i >> 8), (byte)(i >> 16), (byte)(i >> 24)};
	}
	
	public static int bytesToInt(byte[] bs) {
		return (bs[0] & 0xff) | (bs[1] << 8 & 0xff00) | (bs[2] << 16 & 0xff0000) | (bs[3] << 24);
	}
	
	/**
	 * use a simple way to gen the sign
	 * 
	 * retrieve 5 bytes hashed by the length
	 * 
	 * if len &gt; 7
	 * 
	 * x = len / 7;
	 * 
	 * byte 0 =
	 * len - 1 % 2013  ^
	 * 1x + len % 17   ^
	 * 2x + len % 13   ^
	 * 3x + len % 11
	 * 
	 * byte 1 =
	 * 4x - len % 7   ^
	 * 5x - len % 5   ^
	 * 6x - len % 3   ^
	 * 7x - len % 2
	 * 
	 * else
	 * 
	 * byte 0 ^= 
	 * for(x : 0 - n)
	 *  x % 2 == 0
	 *  
	 * byte 1 ^= 
	 * for(x : 0 - n)
	 *  x % 2 == 1
	 * 
	 * @param source
	 * @return
	 */
	public static byte[] genSign(byte[] source) {
		
		if(source != null && source.length > 0) {
			int len = source.length;
			int x = len > 21 ? len / 7 : 3;
			
			return new byte[]{
				(byte)(source[len - 1 % 2013] ^ source[(x + len % 17) % len] ^ source[(2 * x + len % 13) % len] ^ source[(3 * x + len % 11) % len]),
				(byte)(source[(4 * x - len % 7) % len] ^ source[(5 * x - len % 5) % len] ^ source[(6 * x - len % 3) % len] ^ source[(7 * x - 1 - len % 2) % len])	
			};
		} else {
			return new byte[] {0 , 0};
		}
	}
	
	
	public static byte[] genSign(ByteArrays source) {
		
		if(source != null && source.getTotalLength() > 0) {
			int len = source.getTotalLength();
			int x = len > 21 ? len / 7 : 3;
			
			return new byte[]{
				(byte)(source.indexOf(len - 1 % 2013) ^ source.indexOf((x + len % 17) % len) ^ source.indexOf((2 * x + len % 13) % len) ^ source.indexOf((3 * x + len % 11) % len)),
				(byte)(source.indexOf((4 * x - len % 7) % len) ^ source.indexOf((5 * x - len % 5) % len) ^ source.indexOf((6 * x - len % 3) % len) ^ source.indexOf((7 * x - 1 - len % 2) % len))	
			};
		} else {
			return new byte[] {0 , 0};
		}
	}
	
	public static boolean equals(byte[] bytes1, byte[] bytes2) {
		if(bytes1 == bytes2) {
			return true;
		} else if(bytes1 == null || bytes2 == null || bytes1.length != bytes2.length) {
			return false;
		} else {
			for (int i = 0; i < bytes1.length; i++ ) {
				if(bytes1[i] != bytes2[i]) {
					return false;
				}
			}
			
			return true;
 		}
	}
}
