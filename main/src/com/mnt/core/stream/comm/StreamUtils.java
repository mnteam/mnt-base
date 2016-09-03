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

package com.mnt.core.stream.comm;

import com.mnt.core.stream.dtd.ByteArrays.ByteArray;
import com.mnt.core.util.SerializeUtil;
import com.mnt.core.util.ZipUtil;

/**
 * 
 * @author Peng Peng
 *
 */
public class StreamUtils {
	
	private static SerializeHelper serializeHelper;
	private static CompressHelper compressHelper;
	private static boolean enableCompress;
	
	// unit byte, default 100 KB.
	private static int minCompressSize = 1024 * 100; 
	
	public static void registerSerializeHelper(SerializeHelper seriHelper) {
		serializeHelper = seriHelper;
	}
	
	public static void registerCompressHelper(CompressHelper compHelper) {
		compressHelper = compHelper;
	}
	
	public static SerializeHelper getSerializeHelper() {
		if(serializeHelper == null) {
			synchronized (StreamUtils.class) {
				if(serializeHelper == null) {
					serializeHelper = new SerializeHelper() {
						
						@Override
						public byte[] serialize(Object object) {
							return SerializeUtil.serialize(object);
						}
						
						@Override
						public Object deserialize(byte[] source) {
							return SerializeUtil.deSerialize(source);
						}
						
						@Override
						public SerializeHelper getInstance() {
							return this;
						}
					};
				}
			}
		}
		
		return serializeHelper.getInstance();
	}
	
	public static CompressHelper getCompressHelper() {
		if(compressHelper == null) {
			synchronized (StreamUtils.class) {
				if(compressHelper == null) {
					compressHelper = new CompressHelper() {

						@Override
						public byte[] compress(byte[] source) {
							return ZipUtil.compress(source);
						}

						@Override
						public byte[] decompress(byte[] source) {
							return ZipUtil.decompress(source);
						}

						@Override
						public CompressHelper getInstance() {
							return this;
						}

						@Override
						public boolean useByteArray() {
							return false;
						}

						@Override
						public ByteArray compress(ByteArray source) {
							return source;
						}
					};
				}
			}
		}
		
		return compressHelper.getInstance();
	}

	public static void setCompressible(boolean flag) {
		enableCompress = flag;
	}
	
	public static void setMinCompressSize(int minCompSize) {
		minCompressSize = minCompSize;
	}

	public static boolean isEnableCompress() {
		return enableCompress;
	}
	
	public static int getMinCompressSize() {
		return minCompressSize;
	}
}
