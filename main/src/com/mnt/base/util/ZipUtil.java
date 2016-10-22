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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class ZipUtil{
	
	private static final String ZIP_DATA_NAME = "zdn";
	private static final int BUF_SIZE = 64 * 1024;

	public static String compress(String data){
		String result = "";
		if(!CommonUtil.isEmpty(data)){
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(baos));
			zos.setLevel(Deflater.BEST_COMPRESSION);
			
			try{
				zos.putNextEntry(new ZipEntry(ZIP_DATA_NAME));
				zos.write(data.getBytes("iso-8859-1"));
				zos.closeEntry();
				zos.flush();
				zos.finish();
				
				result = baos.toString("iso-8859-1");
			}catch (IOException e){
				result = data;
			}finally{
				try {
					zos.close();
					baos.close();
				} catch (IOException e) {
					// skip it
				}
			}
		}
		
		return result;
	}
	
	public static byte[] compress(byte[] data){
		byte[] result = null;
		if(!CommonUtil.isEmpty(data)){
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(baos));
			zos.setLevel(Deflater.BEST_COMPRESSION);
			
			try{
				zos.putNextEntry(new ZipEntry(ZIP_DATA_NAME));
				zos.write(data);
				zos.closeEntry();
				zos.flush();
				zos.finish();
				
				result = baos.toByteArray();
			}catch (IOException e){
				result = data;
			}finally{
				try {
					zos.close();
					baos.close();
				} catch (IOException e) {
					// skip it
				}
			}
		}
		
		return result;
	}
	
	public static String decompress(String data){
		String result = "";
		if(!CommonUtil.isEmpty(data)){
			ByteArrayInputStream bais = null;
			try {
				bais = new ByteArrayInputStream(data.getBytes("iso-8859-1"));
			} catch (UnsupportedEncodingException e1) {
				// skip it
			}
			ZipInputStream zis = new ZipInputStream(bais);
			
			try{
				ZipEntry entry = zis.getNextEntry();
				if(entry != null){
					if(ZIP_DATA_NAME.equals(entry.getName())){
						byte[] buf = new byte[BUF_SIZE];
						int len = -1;
						StringBuilder sb = new StringBuilder();
						while((len = zis.read(buf)) > 0){
							sb.append(new String(buf, 0, len, "iso-8859-1"));
						}
						
						result = sb.toString();
					}
				}
			} catch(IOException e){
				e.printStackTrace();
				result = data;
			} finally {
				try {
					zis.close();
					bais.close();
				} catch (IOException e) {
					// skip it
				}
			}
		}
		
		return result;
	}
	
	public static byte[] decompress(byte[] data){
		byte[] result = null;
		if(!CommonUtil.isEmpty(data)){
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipInputStream zis = new ZipInputStream(bais);
			
			try{
				ZipEntry entry = zis.getNextEntry();
				if(entry != null){
					if(ZIP_DATA_NAME.equals(entry.getName())){
						byte[] buf = new byte[BUF_SIZE];
						int len = -1;
						while((len = zis.read(buf)) > 0){
							baos.write(buf, 0, len);
						}
						
						result = baos.toByteArray();
					}
				}
			} catch(IOException e){
				e.printStackTrace();
				result = data;
			} finally {
				try {
					baos.close();
					zis.close();
					bais.close();
				} catch (IOException e) {
					// skip it
				}
			}
		}
		
		return result;
	}
	
	/*public static void main(String[] args){
		
		Map<String, Object> dataMap = new HashMap<String, Object>();
		for(int i = 0; i < 1000; i ++){
			dataMap.put("key: " + i, UUID.randomUUID().toString());
		}
		
		long startTime = System.currentTimeMillis();
		
		String data = JSONTool.convertObjectToJson(dataMap);
		
		String cd = compress(data);
		
		String dcd = decompress(cd);
		//System.out.println("co: " + cd.length() + " : " + cd);
		//System.out.println("de: " + dcd.length() + " : " + dcd);
		
		
		JSONTool.convertJsonToObject(dcd);
		//System.out.println(JSONTool.convertJsonToObject(dcd));
		long endTime = System.currentTimeMillis();
		System.out.println(endTime - startTime);
	}*/
}
