package com.mnt.base.evaluator;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class OutputFormatter {
	
	private static final Map<String, Method> FUNC_MAP = new HashMap<String, Method>();
	private Method method;
	
	public OutputFormatter (Method method) {
		this.method = method;
	}
	
	public Object format(Object... params) {
		
		/*if(method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object[].class) {
			try {
				return method.invoke(null, new Object[]{params});
			} catch (Exception e) {
				throw new RuntimeException("error while format the params: " + params + " with the method: " + method.getDeclaringClass().getCanonicalName() + ": " + method.getName() + ", error: " + e.getMessage(), e);
			}
		} else */{
			try {
				return method.invoke(null, params);
			} catch (Exception e) {
				throw new RuntimeException("error while format the params: " + params + " with the method: " + method.getDeclaringClass().getCanonicalName() + ": " + method.getName() + ", error: " + e.getMessage(), e);
			}
		}
		
		
	}

	public static OutputFormatter getByName(String funcName) {
		final Method func = FUNC_MAP.get(funcName);
		if(func != null) {
			return new OutputFormatter(func);
		}
		throw new RuntimeException("no corresponding outputformatter registered for name: " + funcName);
	}

	public static boolean register(String funcName, Method method) {
		if(!FUNC_MAP.containsKey(funcName)) {
			FUNC_MAP.put(funcName, method);
		} 
		
		return false;
	}
}
