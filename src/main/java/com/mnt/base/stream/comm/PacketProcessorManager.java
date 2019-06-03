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

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.dtd.StreamPacket;
import com.mnt.base.util.BaseConfiguration;


/**
 * packet processor manager, start multiple threads to process XMLPacket and send back the response 
 * @author Peng Peng
 * #date 2012-5-10
 *
 */
public abstract class PacketProcessorManager implements Runnable{
	
	private static final Log log = LogFactory.getLog(PacketProcessorManager.class);

	protected Map<Integer, BlockingQueue<StreamPacket>> streamPacketQueueMap;
	
	protected Map<String, PacketProcessor> packetProcessorMap;
	
	private ThreadGroup processorThreadGroup;
	
	protected volatile boolean runningFlag;
	
	protected static boolean enablePacketCacheQueue = !BaseConfiguration.getBoolProperty("disable_packet_cache_queue", false);
	
	protected AtomicLong pushAi;
	protected AtomicLong pollAi;
	protected int maxQueueMapSize;
	
	protected PacketProcessorManager(int threadSize){
		
		
		if(enablePacketCacheQueue) {
			int packetCacheQueueSize = BaseConfiguration.getIntProperty("packet_cache_queue_size", 10000);
			//streamPacketQueue = new LinkedBlockingQueue<StreamPacket>(packetCacheQueueSize);
			
			streamPacketQueueMap = new ConcurrentHashMap<Integer, BlockingQueue<StreamPacket>>();
			this.maxQueueMapSize = threadSize;
			
			this.pushAi = new AtomicLong(0);
			this.pollAi = new AtomicLong(0);
			
			for(int i = 0; i < this.maxQueueMapSize; i++) {
				streamPacketQueueMap.put(i, new LinkedBlockingQueue<StreamPacket>(packetCacheQueueSize));
			}
			
			threadSize = threadSize > 0 ? threadSize : 1; // at least one thread
			initProcessorThreads(threadSize);
		}
		
		initProcessorMap();
	}
	
	private void initProcessorThreads(int maxThreads) {
		processorThreadGroup = new ThreadGroup(PacketProcessorManager.class.getSimpleName());
		processorThreadGroup.setDaemon(true);
		runningFlag = true;
		
		Thread t = null;
		for(int i = 0; i < maxThreads; i++){
			t = new Thread(processorThreadGroup, this);
			t.setDaemon(true);
			t.start();
		}
	}
	
	public void increaseProcessorThreads(int maxThreads) {
		if(runningFlag && enablePacketCacheQueue){
			Thread t = null;
			for(int i = 0; i < maxThreads; i++){
				t = new Thread(processorThreadGroup, this);
				t.setDaemon(true);
				t.start();
			}
		}
	}

	private void initProcessorMap() {
		packetProcessorMap = new ConcurrentHashMap<String, PacketProcessor>();
	}
	
	public void addProcessor(PacketProcessor processor){
		packetProcessorMap.put(processor.processorIdentifier(), processor);
	}
	
	public void pushPacket(StreamPacket streamPacket){
		
		NetTraffic.log("in: ", streamPacket);
		
		if(enablePacketCacheQueue) {
			try {
				streamPacketQueueMap.get((int)Math.abs(pushAi.incrementAndGet() % maxQueueMapSize)).put(streamPacket);
			} catch (InterruptedException e) {
				log.error("Error while put the stream packet in processor queue");
			}
		} else {
			try {
				dispatchPacket(streamPacket);
			} catch(Exception e) {
				log.error("error while dispatch packet: " + streamPacket.toString(), e);
			}
			
		}
	}

	@Override
	public void run() {
		while(runningFlag){
			StreamPacket streamPacket = null;
			try {
				streamPacket = streamPacketQueueMap.get((int)Math.abs(pollAi.incrementAndGet() % maxQueueMapSize)).poll(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				log.error("Error while process the stream packet in processor queue", e);
				Thread.currentThread().interrupt();
				runningFlag = false;
			}
			
			if(streamPacket != null){
				
				NetTraffic.log("ready to process: ", streamPacket);
				
				try {
					dispatchPacket(streamPacket);
				} catch(Exception e) {
					log.error("error while dispatch packet: " + streamPacket.toString(), e);
				}
			}
			
			NetTraffic.log("processed: ", streamPacket);
		}
	}

	protected abstract void dispatchPacket(StreamPacket streamPacket);
}
