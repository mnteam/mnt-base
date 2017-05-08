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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * named lock for get the static lock object
 * @author Peyton Peng
 * #date 2012-4-13
 *
 *
 */
public final class NamedSyncTarget {
	private static final Map<String, Object> memMap = new ConcurrentHashMap<String, Object>();
	
	private static final Object targetCreateLocker = new Object();
	
	private AtomicLong lockCount;
	
	private NamedSyncTarget(String lockType, String lockName){
		lockCount = new AtomicLong();
		MapUtil.putValue(memMap, this, lockType, lockName);
	}
	
	public static final NamedSyncTarget getTarget(String lockType, String lockName){
		if(MapUtil.containsKey(memMap, lockType, lockName)){
			NamedSyncTarget nl = (NamedSyncTarget)MapUtil.getValue(memMap, lockType, lockName);
			
			if(nl == null) {
				return getTarget(lockType, lockName);
			}
			
			nl.lockCount.incrementAndGet();
			return nl;
		}else{
			synchronized (targetCreateLocker) {
				if(MapUtil.containsKey(memMap, lockType, lockName)){
					return getTarget(lockType, lockName);
				}
				
				return new NamedSyncTarget(lockType, lockName);
			}
		}
	}
	
	public static final void releaseTarget(String lockType, String lockName){
		
		NamedSyncTarget nl = (NamedSyncTarget)MapUtil.getValue(memMap, lockType, lockName);
		
		if(nl != null && (nl.lockCount.decrementAndGet()) <= 0){
			MapUtil.remove(memMap, lockType, lockName);
		}
	}
}
