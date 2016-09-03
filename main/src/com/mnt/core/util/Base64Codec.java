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

package com.mnt.core.util;

/**
 * Providing the method for base64 encode\decode
 *
 */
public class Base64Codec {
	
	private static final byte[] ENCODING_TABLE = {
		(byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E',
		(byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J',
		(byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O',
		(byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T',
		(byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y',
		(byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd',
		(byte) 'e', (byte) 'f', (byte) 'g', (byte) 'h', (byte) 'i',
		(byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n',
		(byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's',
		(byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x',
		(byte) 'y', (byte) 'z', (byte) '0', (byte) '1', (byte) '2',
		(byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
		(byte) '8', (byte) '9', (byte) '+', (byte) '/'
	};
    private static final byte[] DECODING_TABLE;
    
	static {
		DECODING_TABLE = new byte[128];
		for (int i = 0; i < 128; i++) {
			DECODING_TABLE[i] = (byte) -1;
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			DECODING_TABLE[i] = (byte) (i - 'A');
		}
		for (int i = 'a'; i <= 'z'; i++) {
			DECODING_TABLE[i] = (byte) (i - 'a' + 26);
		}
		for (int i = '0'; i <= '9'; i++) {
			DECODING_TABLE[i] = (byte) (i - '0' + 52);
		}
		DECODING_TABLE['+'] = 62;
		DECODING_TABLE['/'] = 63;
	}
    
    public static byte[] encode(final byte[] data) {
        byte[] bytes;
        int modulus = data.length % 3;
        if (modulus == 0) {
            bytes = new byte[(4 * data.length) / 3];
        } else {
            bytes = new byte[4 * ((data.length / 3) + 1)];
        }
        int dataLength = (data.length - modulus);
        int a1;
        int a2;
        int a3;
        for (int i = 0, j = 0; i < dataLength; i += 3, j += 4) {
            a1 = data[i] & 0xff;
            a2 = data[i + 1] & 0xff;
            a3 = data[i + 2] & 0xff;
            bytes[j] = ENCODING_TABLE[(a1 >>> 2) & 0x3f];
            bytes[j + 1] = ENCODING_TABLE[((a1 << 4) | (a2 >>> 4)) & 0x3f];
            bytes[j + 2] = ENCODING_TABLE[((a2 << 2) | (a3 >>> 6)) & 0x3f];
            bytes[j + 3] = ENCODING_TABLE[a3 & 0x3f];
        }
        int b1;
        int b2;
        int b3;
        int d1;
        int d2;
        switch (modulus) {
        case 0: /* nothing left to do */
            break;
        case 1:
            d1 = data[data.length - 1] & 0xff;
            b1 = (d1 >>> 2) & 0x3f;
            b2 = (d1 << 4) & 0x3f;
            bytes[bytes.length - 4] = ENCODING_TABLE[b1];
            bytes[bytes.length - 3] = ENCODING_TABLE[b2];
            bytes[bytes.length - 2] = (byte) '=';
            bytes[bytes.length - 1] = (byte) '=';
            break;
        case 2:
            d1 = data[data.length - 2] & 0xff;
            d2 = data[data.length - 1] & 0xff;
            b1 = (d1 >>> 2) & 0x3f;
            b2 = ((d1 << 4) | (d2 >>> 4)) & 0x3f;
            b3 = (d2 << 2) & 0x3f;
            bytes[bytes.length - 4] = ENCODING_TABLE[b1];
            bytes[bytes.length - 3] = ENCODING_TABLE[b2];
            bytes[bytes.length - 2] = ENCODING_TABLE[b3];
            bytes[bytes.length - 1] = (byte) '=';
            break;
        }
        return bytes;
    }
    
	public static String encode(final String data){
		return new String(encode(data.getBytes()));
	}
	
	public static String encode(final String data, final String charset){
		if(charset != null){
			try{
				return new String(encode(data.getBytes(charset)));
			}
			catch(Throwable e){
				throw new RuntimeException(e);
			}
		}
		else{
			return new String(encode(data.getBytes()));
		}
	}
	
    public static byte[] decode(final byte[] data) {
        byte[] bytes;
        byte b1;
        byte b2;
        byte b3;
        byte b4;
        if (data[data.length - 2] == '=') {
            bytes = new byte[(((data.length / 4) - 1) * 3) + 1];
        } else if (data[data.length - 1] == '=') {
            bytes = new byte[(((data.length / 4) - 1) * 3) + 2];
        } else {
            bytes = new byte[((data.length / 4) * 3)];
        }
        
        for (int i = 0, j = 0; i < (data.length - 4); i += 4, j += 3) {
            b1 = DECODING_TABLE[data[i]];
            b2 = DECODING_TABLE[data[i + 1]];
            b3 = DECODING_TABLE[data[i + 2]];
            b4 = DECODING_TABLE[data[i + 3]];
            bytes[j] = (byte) ((b1 << 2) | (b2 >> 4));
            bytes[j + 1] = (byte) ((b2 << 4) | (b3 >> 2));
            bytes[j + 2] = (byte) ((b3 << 6) | b4);
        }
        
        if (data[data.length - 2] == '=') {
            b1 = DECODING_TABLE[data[data.length - 4]];
            b2 = DECODING_TABLE[data[data.length - 3]];
            bytes[bytes.length - 1] = (byte) ((b1 << 2) | (b2 >> 4));
        } else if (data[data.length - 1] == '=') {
            b1 = DECODING_TABLE[data[data.length - 4]];
            b2 = DECODING_TABLE[data[data.length - 3]];
            b3 = DECODING_TABLE[data[data.length - 2]];
            bytes[bytes.length - 2] = (byte) ((b1 << 2) | (b2 >> 4));
            bytes[bytes.length - 1] = (byte) ((b2 << 4) | (b3 >> 2));
        } else {
            b1 = DECODING_TABLE[data[data.length - 4]];
            b2 = DECODING_TABLE[data[data.length - 3]];
            b3 = DECODING_TABLE[data[data.length - 2]];
            b4 = DECODING_TABLE[data[data.length - 1]];
            bytes[bytes.length - 3] = (byte) ((b1 << 2) | (b2 >> 4));
            bytes[bytes.length - 2] = (byte) ((b2 << 4) | (b3 >> 2));
            bytes[bytes.length - 1] = (byte) ((b3 << 6) | b4);
        }
        return bytes;
    }
    
	public static String decode(final String data){
		return new String(decode(data.getBytes()));
	}
	
	public static String decode(final String data, final String charset){
		if(charset != null) {
			try{
				return new String(decode(data.getBytes()), charset);
			}
			catch(Throwable e) {
				throw new RuntimeException(e);
			}
		} else {
			return new String(decode(data.getBytes()));
		}
	}
	
	public static String decodeAfterDiscardNonBase64Bytes(final String data, final String charset){
		if(charset != null) {
			try{
				return new String(decodeAfterDiscardNonBase64Bytes(data.getBytes()), charset);
			}
			catch(Throwable e) {
				throw new RuntimeException(e);
			}
		} else {
			return new String(decode(data.getBytes()));
		}
	}
	
	public static String decodeAfterDiscardNonBase64Bytes(final String data){
		return new String(decodeAfterDiscardNonBase64Bytes(data.getBytes()));
	}
	
	public static byte[] decodeAfterDiscardNonBase64Bytes(final byte[] bytesData) {
		return decode(discardNonBase64Bytes(bytesData));
	}
    
	private static byte[] discardNonBase64Bytes(final byte[] data) {
		byte[] temp = new byte[data.length];
		int bytesCopied = 0;
		for (int i = 0; i < data.length; i++) {
			if (isValidBase64Byte(data[i])) {
				temp[bytesCopied++] = data[i];
			}
		}
		byte[] newData = new byte[bytesCopied];
		System.arraycopy(temp, 0, newData, 0, bytesCopied);
		return newData;
	}
	
	private static boolean isValidBase64Byte(final byte b) {
		if (b == '=') {
			return true;
		} else if ((b < 0) || (b >= 128)) {
			return false;
		} else if (DECODING_TABLE[b] == -1) {
			return false;
		}
		return true;
	}
}
