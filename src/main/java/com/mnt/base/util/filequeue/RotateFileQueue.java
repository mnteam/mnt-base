package com.mnt.base.util.filequeue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
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
 * 允许使用多个文件进行轮转，最少2个
 * 
 * @author Peng Peng
 * #date 2017年1月25日
 * @version 1.0
 * since JDK 1.8
 *
 * @param <T>
 */
public class RotateFileQueue<T> {
	
	private Serializier<T> serializer;
	
	private RotateFile rFile;
	
	public static final byte DATA 		= 1;
	public static final byte NO_DATA 	= 0;
	
	public static final byte[] DATA_ARR = {DATA};
	
	private static final int BUFFER_SIZE = 1024 * 1024; // buff: 1m
	
	private class RotateFile implements Runnable {
		
		Object writeLock = new Object();
		Object readLock = new Object();
		
		Object rotateLock = new Object();
		
		int maxAllowedSize;
		int fileQueueSize = 2;
		
		List<File> fQueue;
		AtomicLong readFileRotateIndex = new AtomicLong(0);
		AtomicLong writeFileRotateIndex = new AtomicLong(0);
		
		AtomicInteger dataReadIndex = new AtomicInteger();
		AtomicInteger dataWriteIndex = new AtomicInteger();
		
		RandomAccessFile readFile;
		OutputStream writeFile;
		
		RotateFile(String filePath, int maxAllowedSize, int fileQueueSize) {
			
			if(fileQueueSize < 2 || maxAllowedSize < 1024) {
				throw new IllegalArgumentException("[#RotateFile]fileQueueSize should be greater than 2 and maxAllowedSize should be greater than 1024.");
			}
			
			this.maxAllowedSize = maxAllowedSize;
			this.fileQueueSize = fileQueueSize;
			
			fQueue = new ArrayList<File>(fileQueueSize);
			for(int i = 0; i < fileQueueSize; i++) {
				fQueue.add(new File(filePath + "." + i));
			}
			
			//清除如果存在的任何文件
			for(File f : fQueue) {
				if(f.exists()) {
					f.delete();
				}
			}
			
			Thread t = new Thread(this);
			t.setDaemon(true);
			t.start();
		}
		
		File currentFile(boolean isRead) {
			return fQueue.get((int)(((isRead ? readFileRotateIndex.get() : writeFileRotateIndex.get()) - 1) % fileQueueSize));
		}
		
		File nextFile(boolean isRead) {
			return fQueue.get((int)(((isRead ? readFileRotateIndex.get() : writeFileRotateIndex.get())) % fileQueueSize));
		}
		
		RandomAccessFile nextReadFile() {
			
			try {
				if(readFile != null) {
					readFile.close();
					currentFile(true).delete();
				}
				
				if(!nextFile(true).exists()) {
					
					synchronized (rotateLock) {
						try {
							rotateLock.wait(100);
						} catch (InterruptedException e) {
							throw new RuntimeException("error while wait the next read file.", e);
						}
					}
				}
				readFile = new RandomAccessFile(nextFile(true), "r");
				
				readFileRotateIndex.getAndIncrement();
				
				synchronized (rotateLock) {
					rotateLock.notify();
				}
			} catch (IOException e) {
				throw new RuntimeException("error while get the next read file.", e);
			}
			
			return readFile;
		}
		
		RandomAccessFile readFile() {
			if(readFile == null) {
				nextReadFile();
			}
			
			return readFile;
		}
		
		public byte[] readData() {
			RandomAccessFile raf = readFile();
			if(readBytes(raf, 1)[0] == DATA) {
				byte[] lenBs = readBytes(raf, 4);
				int len = bytesToInt(lenBs);
				
				byte[] buff = readBytes(raf, len);
				dataReadIndex.addAndGet(5 + len);
				
				return buff;
			} else {
				// 重置游标
				nextReadFile();
				dataReadIndex.set(0);
				
				return readData();
			}
		}
		
		byte[] readBytes(RandomAccessFile raf, int len) {
			byte[] buf = new byte[len];
			int start = 0, delta;
			do {
				try {
					delta = raf.read(buf, start, len - start);
					if(delta > 0) {
						start += delta;
					}
				} catch (IOException e) {
					throw new RuntimeException("error while read data from file.", e);
				}
			} while(len - start > 0);
			
			return buf;
		}

		OutputStream writeFile() {
			if(writeFile == null) {
				nextWriteFile();
			}
			
			return writeFile;
		}
		
		OutputStream nextWriteFile() {
			try {
				if(writeFile != null) {
					writeFile.close();
				}
				
				// 如果接下来要写的文件是当前正在读的文件，暂停等待读文件转移到下一个rotateIndex
				if(nextFile(false).exists()) {
					synchronized (rotateLock) {
						try {
							rotateLock.wait(100);
						} catch (InterruptedException e) {
							throw new RuntimeException("error while wait the next write file.", e);
						}
					}
				}
				
				writeFile = new BufferedOutputStream(new FileOutputStream(nextFile(false)), BUFFER_SIZE);
			
				writeFileRotateIndex.incrementAndGet();
				
				synchronized (rotateLock) {
					rotateLock.notify();
				}
			} catch (IOException e) {
				throw new RuntimeException("error while get the next write file.", e);
			}
			
			return writeFile;
		}

		public void writeData(byte[] bs) {
			
			int requiredSize = bs.length + 5;
			
			if(requiredSize > maxAllowedSize) {
				throw new RuntimeException("required data size " + requiredSize + " exceed the max allowed size: " + maxAllowedSize);
			}
			
			try {
				if(dataWriteIndex.get() + requiredSize > maxAllowedSize) {
					writeFile().write(NO_DATA);
					nextWriteFile();
					dataWriteIndex.set(0);
				}
				
				OutputStream os = writeFile();
				
				os.write(DATA);
				os.write(intToBytes(bs.length));
				os.write(bs);
				
				dataWriteIndex.addAndGet(requiredSize);
			} catch (IOException e1) {
				throw new RuntimeException("error while write data to file: " + e1.getMessage(), e1);
			}
		}

		public void close() {
			try {
				readFile.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				writeFile.close();
			} catch (IOException e) {
				// ignore
			}
		}

		@Override
		public void run() {
			while(true) {
				// 每10秒强制刷新数据到缓存
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					break;
				}
				
				OutputStream out = writeFile();
				if(out != null) {
					try {
						out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public RotateFileQueue(String filePath, int maxAllowedSize, int fileQueueSize, Serializier<T> serializer) {
		this.serializer = serializer;
		rFile = new RotateFile(filePath, maxAllowedSize, fileQueueSize);
	}
	
	public void put(T e) {
		synchronized (rFile.writeLock) {
			byte[] bs = serializer.serialize(e);
			rFile.writeData(bs);
		}
	}

	public T poll() {
		T t = null;
		synchronized (rFile.readLock) {
			byte[] bs = rFile.readData();
			t = serializer.deserialize(bs);
		}
		
		return t;
	}
	
	public void close() {
		rFile.close();
	}
	
	public static byte[] intToBytes(int i) {
		return new byte[]{(byte)(i), (byte)(i >> 8), (byte)(i >> 16), (byte)(i >> 24)};
	}
	
	public static int bytesToInt(byte[] bs) {
		return (bs[0] & 0xff) | (bs[1] << 8 & 0xff00) | (bs[2] << 16 & 0xff0000) | (bs[3] << 24);
	}
	
	/*public static void main(String[] args) throws IOException {
		RotateFileQueue<String> fq = new RotateFileQueue<>("/Users/webull/ws_java/test/conf/tmp", 1024, 5, new Serializier<String>() {
			
			@Override
			public byte[] serialize(String t) {
				return t.getBytes();
			}
			
			@Override
			public String deserialize(byte[] bs) {
				return new String(bs);
			}
		});
		
		new Thread(new Runnable() {
			public void run() {
				int j = 0;
				while(j ++ < 1000) {
					System.out.println(fq.poll());
				}
			}}).start();
		
		int i = 0;
		while(i++ < 1000) {
			fq.put("data val: " + i + "测试s> " + Math.random() * 10);
		}
		
		System.in.read();
		
		fq.close();
	}*/
}
