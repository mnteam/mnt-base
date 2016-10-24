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

package com.mnt.base.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <pre>
 * Hash tool for string hash and file hash with different message digest.
 * 
 * Currently support MD5 and SHA.
 * </pre>
 * 
 * @author Peng Peng
 * #date 2013-2-17
 *
 */
public class HashUtil {
	
	private static final Log log 		= LogFactory.getLog(HashUtil.class);
	
	private static final String MD5		= "MD5";
	private static final String SHA 	= "SHA";
	private static final char DIGITS[]	= { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static String toHexString(byte[] data) {
		StringBuilder sb = new StringBuilder(data.length * 2);
		
		for (int i = 0; i < data.length; i++) {
			sb.append(DIGITS[(data[i] & 0xf0) >>> 4]);
			sb.append(DIGITS[data[i] & 0x0f]);
		}
		
		Integer.toHexString(1);
		
		return sb.toString();
	}
	
	/**
	 * Hash string by SHA algorithm
	 * 
	 * @param source
	 * @return
	 */
	public static String hashBySHA(String source) {
		return hashBy(source, SHA);
	}
	
	/**
	 * Hash string by MD5 algorithm
	 * 
	 * @param source
	 * @return
	 */
	public static String hashByMD5(String source) {
		return hashBy(source, MD5);
	}
	
	/**
	 * Hash file by SHA algorithm
	 * 
	 * @param fileName
	 * @return
	 */
	public static String hashFileBySHA(String fileName) {
		return hashFile(fileName, SHA);
	}
	
	/**
	 * Hash stream by SHA algorithm
	 * 
	 * @param input
	 * @return
	 */
	public static String hashStreamBySHA(InputStream input) {
		return hashStream(input, SHA);
	}
	
	/**
	 * Hash file by MD5 algorithm
	 * 
	 * @param fileName
	 * @return
	 */
	public static String hashFileByMD5(String fileName) {
		return hashFile(fileName, MD5);
	}
	
	/**
	 * Hash stream by MD5 algorithm
	 * 
	 * @param input
	 * @return
	 */
	public static String hashStreamByMD5(InputStream input) {
		return hashStream(input, MD5);
	}

	public static String hashBy(String source, String mdType) {
		
		if(source != null) {
			MessageDigest digest = getMessageDigest(mdType);

			if(digest != null) {
				digest.reset();
				digest.update(source.getBytes());
				
				return toHexString(digest.digest());
			} else {
				return source;
			}
		}
		
		return null;
	}

	/**
	 * Hash file by file name and specified message digest type.
	 * 
	 * @param fileName
	 * @param mdType
	 * @return
	 */
	public static String hashFile(String fileName, String mdType) {
		InputStream input = null;
		
		try {
			input = new FileInputStream(fileName);
			return hashStream(input, mdType);
		} catch (IOException e) {
			log.error(new StringBuilder("Fail to hash while loading the file : ").append(fileName), e);
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch (IOException e) {
					log.error(new StringBuilder("Fail to close the file while hash file with mdType: ").append(mdType), e);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Hash Stream by stream and specified message digest type.
	 * 
	 * @param input
	 * @param mdType
	 * @return
	 */
	public static String hashStream(InputStream input, String mdType) {
		
		MessageDigest digest = getMessageDigest(mdType);
		
		if(input != null && digest != null) {
			
			byte[] buff = new byte[10240];
			int len = 0;
			
			try {
				while ((len = input.read(buff)) > 0) {
					digest.update(buff, 0, len);
				}
				return toHexString(digest.digest());
			} catch (Exception e) {
				log.error(new StringBuilder("Fail to hash input by mdType: ").append(mdType), e);
			}
		}
		
		return null;
	}
	
	private static MessageDigest getMessageDigest(String mdType) {
		try {
			return MessageDigest.getInstance(mdType);
		} catch (NoSuchAlgorithmException e) {
			log.warn(new StringBuilder("Can not get the ").append(mdType).append(" digest instance.").toString(), e);
		}
		
		return null;
	}
	
	/*public static void main(String[] args) {
		//test only
	}*/
}
