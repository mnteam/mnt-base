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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The cache manager implementation for cache support. (It would be implmenets with the memcache in the future.)
 * 
 * @author Peng Peng
 * #date 2012-03-31
 *
 *
 */
public abstract class CacheManager<K, V> {

	public static <K, V> CacheManager<K, V> getInstance(){
		return new MemCacherManager<K, V>();
	}
	
	public static <K, V> CacheManager<K, V> getInstance(int capacity){
		if(capacity < 1){
			throw new InvalidParameterException("The capacity must be over zero.");
		}
		return new CapacityCacherManager<K, V>(capacity);
	}
	
	private static class MemCacherManager<K, V> extends CacheManager<K, V>{

		private Map<K, V> cacherMap = new ConcurrentHashMap<K, V>();
		
		@Override
		public V get(K k) {
			return cacherMap.get(k);
		}

		@Override
		public V put(K k, V v) {
			return cacherMap.put(k, v);
		}

		@Override
		public V remove(K k) {
			return cacherMap.remove(k);
		}

		@Override
		public void clear() {
			cacherMap.clear();
		}

		@Override
		public boolean containsKey(Object k) {
			return cacherMap.containsKey(k);
		}
		
		@Override
		public Collection<V> values() {
			return cacherMap.values();
		}
		
		@Override
		public Set<K> keySets() {
			return cacherMap.keySet();
		}
	}
	
	private static class CapacityCacherManager<K, V> extends CacheManager<K, V>{

		private Map<K, V> cacherMap;
		private int capacity;
		
		private LinkedHashSet<K> cachekeys;
		private void touch(K k) {
			// skip the times while clean
			if(readyFlag) {
				cachekeys.add(k);
			}
			
			if(cachekeys.size() > capacity) {
				startClean();
			}
		}
		
		boolean readyFlag = true;
		public void startClean() {
			if(readyFlag) {
				readyFlag = false;
				cleanExceedKeys();
			}
		}
		
		public void cleanExceedKeys() {
			new Thread() {
				public void run() {
					int keepSize = capacity / 3;
					
					List<K> kList = new ArrayList<K>();
					kList.addAll(cachekeys);
					//Collections.reverse(kList);
					
					K k;
					for(int i = 0; i < keepSize; i++) {
						k = kList.get(i);
						cachekeys.remove(k);
						cacherMap.remove(k);
					}
					kList.clear();
					readyFlag = true;
				}
			}.start();
		}
		
		private CapacityCacherManager(int capacity){
			this.capacity = (int)(capacity * 1.5);
			this.cacherMap = new ConcurrentHashMap<K, V>();
			cachekeys = new LinkedHashSet<K>();
		}
		
		@Override
		public V get(K k) {
			V v = cacherMap.get(k);
			if(v != null){
				touch(k);
			}
			
			return v;
		}

		@Override
		public V put(K k, V v) {
			touch(k);
			return cacherMap.put(k, v);
		}
		
		@Override
		public V remove(K k) {
			return cacherMap.remove(k);
		}

		@Override
		public void clear() {
			cacherMap.clear();
		}
		
		@Override
		public boolean containsKey(Object k) {
			return cacherMap.containsKey(k);
		}

		@Override
		public Collection<V> values() {
			throw new java.lang.UnsupportedOperationException("CapacityCacherManager can not retrieve all values.");
		}

		@Override
		public Set<K> keySets() {
			throw new java.lang.UnsupportedOperationException("CapacityCacherManager can not retrieve all keyset.");
		}
	}
	
	public abstract V get(K k);
	
	public abstract V put(K k, V v);
	
	public abstract V remove(K k);
	
	public abstract void clear();
	
	public abstract boolean containsKey(Object k);
	
	public abstract Collection<V> values();
	
	public abstract Set<K> keySets();
	
	/*public static void main(String[] args) throws Exception {
		CacheManager<String, Integer> cm = CacheManager.getInstance(10);
		cm.put("a1", 1);
		cm.put("a2", 2);
		cm.put("a3", 3);
		cm.put("a4", 4);
		cm.put("a5", 5);
		cm.put("a6", 6);
		cm.put("a7", 7);
		cm.put("a8", 8);
		cm.put("a9", 9);
		cm.put("a10", 10);
		cm.put("a11", 11);
		cm.put("a12", 12);
		cm.put("a13", 13);
		cm.put("a14", 14);
		cm.put("a15", 15);
		cm.put("a16", 16);
		cm.put("a17", 17);
		cm.put("a18", 18);
		
		Thread.sleep(1000);
		
		System.out.println(cm.get("a1"));
		System.out.println(cm.get("a2"));
		System.out.println(cm.get("a3"));
		System.out.println(cm.get("a4"));
		System.out.println(cm.get("a5"));
		System.out.println(cm.get("a6"));
		System.out.println(cm.get("a7"));
		System.out.println(cm.get("a8"));
		System.out.println(cm.get("a9"));
		System.out.println(cm.get("a10"));
		System.out.println(cm.get("a11"));
		System.out.println(cm.get("a12"));
		System.out.println(cm.get("a13"));
		System.out.println(cm.get("a14"));
		System.out.println(cm.get("a15"));
		System.out.println(cm.get("a16"));
		System.out.println(cm.get("a17"));
		System.out.println(cm.get("a18"));
	}*/
}
