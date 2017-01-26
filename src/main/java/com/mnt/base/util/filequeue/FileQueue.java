package com.mnt.base.util.filequeue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件队列，数据入队列 入队列模式：
 * 
 * 标志位         数据长度        数据
 * 1 表示有数据  
 * 0 表示结束
 * [1]           [4]           [n]
 * 
 * 
 * @author Peng Peng
 * @date 2017年1月25日
 * @version 1.0
 * since JDK 1.8
 *
 * @param <T>
 */
public class FileQueue<T> {
	
	private Serializier<T> serializer;
	
	private Cursor writeCursor;
	private Cursor readCursor;

	private int maxAllowedSize;
	
	private RandomAccessFile queueFileRaf;
	
	public static final byte DATA 		= 1;
	public static final byte NO_DATA 	= 0;
	
	public static final byte[] DATA_ARR = {DATA};
	
	public FileQueue(String filePath, int maxAllowedSize, Serializier<T> serializer) {
		
		File f = new File(filePath);
		if(f.exists()) {
			f.delete();
		}
		
		MappedByteBuffer writeMap, readMap;
		try {
			queueFileRaf = new RandomAccessFile(filePath, "rw");
			
			FileChannel ch = queueFileRaf.getChannel();
			
			writeMap = ch.map(MapMode.READ_WRITE, 0, maxAllowedSize);
			readMap = ch.map(MapMode.READ_ONLY, 0, maxAllowedSize);
			
		} catch (IOException e) {
			throw new RuntimeException("error while create queue file with path: " + filePath, e);
		}
		
		this.maxAllowedSize = maxAllowedSize;
		this.writeCursor = new Cursor(writeMap);
		this.readCursor = new Cursor(readMap);
		this.serializer = serializer;
	}
	
	public void put(T e) {
		synchronized (writeCursor.lock) {
			byte[] bs = serializer.serialize(e);
			int requiredSize = bs.length + 5;
			if(!(allowWrite(requiredSize))) {
				while(!(allowWrite(requiredSize))) {
					synchronized (writeCursor) {
						writeCursor.waitTo(100);
					}
				}
			}
			
			writeCursor.checkWrite(requiredSize);
			writeCursor.write(DATA_ARR, intToBytes(bs.length), bs);
			
			readCursor.checkWait();
		}
	}
	
	private boolean allowWrite(int requiredSize) {
		if(requiredSize > maxAllowedSize) {
			throw new RuntimeException("required data size " + requiredSize + " exceed the max allowed size in queue file: " + maxAllowedSize);
		}
		
		return (writeCursor.realIndex() - readCursor.realIndex() + requiredSize) < maxAllowedSize;
	}

	public T poll() {
		T t = null;
		synchronized (readCursor.lock) {
			if(!(allowRead())) {
				while(!(allowRead())) {
					synchronized (readCursor) {
						readCursor.waitTo(100);
					}
				}
			}
		}
		
		byte[] bs = readCursor.readData();
		
		// 页面切换点，递归调用，判断allowRead
		if(bs == null) {
			return poll();
		}
		
		writeCursor.checkWait();
		
		t = serializer.deserialize(bs);
		
		return t;
	}
	
	private boolean allowRead() {
		return writeCursor.realIndex() > readCursor.realIndex();
	}

	public void close() {
		try {
			queueFileRaf.close();
		} catch (IOException e) {
			// ignore
		}
	}
	
	private class Cursor {
		volatile boolean waitFlag;
		AtomicLong loopTimes = new AtomicLong();
		AtomicInteger index = new AtomicInteger();
		Object lock = new Object();
		private MappedByteBuffer queueBuff;
		
		Cursor(MappedByteBuffer queueBuff) {
			this.queueBuff = queueBuff;
		}

		public byte[] readData() {
			
			byte[] buf = null;
			
			if(readByte() == 1) {
				byte[] lenBytes = {readByte(), readByte(), readByte(), readByte()};
				int len = bytesToInt(lenBytes);
				
				buf = new byte[len];
				
				queueBuff.get(buf, 0, len);
				
				index.addAndGet(5 + len);
			} else {
				loopTimes.incrementAndGet();
				index.set(0);
				
				queueBuff.position(0);
			}
			return buf;
		}
		
		byte readByte() {
			return queueBuff.get();
		}

		void waitTo(int timeout) {
			this.waitFlag = true;
			try {
				this.wait(timeout);
			} catch (InterruptedException e1) {
				throw new RuntimeException("error while wait cursor available to write.", e1);
			}
		}
		
		void checkWait() {
			if(waitFlag) {
				synchronized (this) {
					this.notify();
					this.waitFlag = false;
				}
			}
		}

		void checkWrite(int requiredSize) {
			// 超过了fileQueue最大允许的大小
			if(requiredSize + index.get() > maxAllowedSize) {
				loopTimes.incrementAndGet();
				write(NO_DATA);
				// must be after the write to reset the data
				index.set(0);
				
				queueBuff.position(0);
			}
		}
		
		void write(byte[]... bs) {
			
			int totalLen = 0;
			for(int i = 0; i < bs.length; i++) {
				queueBuff.put(bs[i]);
				totalLen += bs[i].length;
			}
			index.addAndGet(totalLen);
		}

		void write(byte... bs) {
			queueBuff.put(bs);
			index.addAndGet(bs.length);
		}
		
		long realIndex() {
			return index.get() + loopTimes.get() * maxAllowedSize;
		}
	}
	
	public static byte[] intToBytes(int i) {
		return new byte[]{(byte)(i), (byte)(i >> 8), (byte)(i >> 16), (byte)(i >> 24)};
	}
	
	public static int bytesToInt(byte[] bs) {
		return (bs[0] & 0xff) | (bs[1] << 8 & 0xff00) | (bs[2] << 16 & 0xff0000) | (bs[3] << 24);
	}
	
	public static void main(String[] args) throws IOException {
		FileQueue<String> fq = new FileQueue<>("/Users/webull/ws_java/test/conf/tmp", 100, new Serializier<String>() {
			
			@Override
			public byte[] serialize(String t) {
				return t.getBytes();
			}
			
			@Override
			public String deserialize(byte[] bs) {
				return new String(bs);
			}
		});
		
		new Thread(()->{
			int j = 0;
			while(j ++ < 1000) {
				System.out.println(fq.poll());
			}
		}).start();
		
		int i = 0;
		while(i++ < 1000) {
			fq.put("data val: " + i + "测试s> " + Math.random() * 10);
		}
		
		System.in.read();
		
		fq.close();
	}
}
