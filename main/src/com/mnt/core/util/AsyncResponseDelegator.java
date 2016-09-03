package com.mnt.core.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncResponseDelegator {
	
	private static final Object NULL_OBJ = new Object();

	private static Map<Object, Object> resultCollector = new ConcurrentHashMap<Object, Object>();

	private static Map<Object, Object> resultCollectorLocker = new ConcurrentHashMap<Object, Object>();
	
	public static boolean putResult(Object requestId, Object result) {
		
		boolean resultFlag = false;
		Object lckObj = resultCollectorLocker.get(requestId);
		
		// the request is timeout or not exists
		if(lckObj != null) {
			
			if(result != null) {
				resultCollector.put(requestId, result);
			}
			
			synchronized(lckObj) {
				lckObj.notify();
			}
			resultFlag = true;
		} else {
			resultCollector.remove(requestId);
		}
		
		return resultFlag;
	}
	
	public static Object createWaitingLock(Object requestId) {
		Object lckObj = new Object();
		resultCollectorLocker.put(requestId, lckObj);
		return lckObj;
	}
	
	public static Object waitResult(Object requestId, Object lckObj) {
		while(!resultCollector.containsKey(requestId)) {
			synchronized (lckObj) {
				try {
					lckObj.wait(1000);
				} catch (InterruptedException e) {
					// skip any exception here
				}
			}
		}
		
		resultCollectorLocker.remove(requestId);
		Object result = resultCollector.remove(requestId);
		return result == NULL_OBJ ? null : result;
	}

	public static Object waitResult(Object requestId) {
		Object lckObj = new Object();
		resultCollectorLocker.put(requestId, lckObj);

		while(!resultCollector.containsKey(requestId)) {
			synchronized (lckObj) {
				try {
					lckObj.wait(1000);
				} catch (InterruptedException e) {
					// skip any exception here
				}
			}
		}
		
		resultCollectorLocker.remove(requestId);
		Object result = resultCollector.remove(requestId);
		return result == NULL_OBJ ? null : result;
	}

	public static void clearWaiting(List<Object> requestIds) {
		if(!CommonUtil.isEmpty(requestIds)) {
			for(Object requestId : requestIds) {
				putResult(requestId, null);
			}
		}
	}
}
