package com.mnt.base.stream.comm;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.stream.annotation.ProcessorMethod;

public abstract class AnnotatedPacketProcessor implements PacketProcessor {
	
	private static final Log log = LogFactory.getLog(AnnotatedPacketProcessor.class);

	private Map<String, Method> methodMap;
	
	public AnnotatedPacketProcessor() {
		methodMap = new HashMap<String, Method>();
		initAnnoations();
	}

	private void initAnnoations() {
		
		Method[] methods = getClass().getDeclaredMethods();
		for(Method m : methods) {
			ProcessorMethod processorMethod = m.getAnnotation(ProcessorMethod.class);
			
			if(processorMethod != null) {
				m.setAccessible(true);
				methodMap.put(processorMethod.identifier(), m);
			}
		}
	}

	@Override
	public Object prcocessPacket(String requestId, String methodIdentifier,
			Object parameters) {
		
		Method method = methodMap.get(methodIdentifier);
		
		if(method == null) {
			method = methodMap.get(ProcessorMethod.DEFAULT_IDENTIFIER);
		}
		
		if(method != null) {
			try {
				return method.invoke(this, new Object[]{requestId, parameters});
			} catch (Exception e) {
				log.error("error while invoke the method: " + method.getName() + " for class: "  + this.getClass().getName() + ", requestId: " + requestId +", parameters: " + parameters, e);
			}
		} else {
			log.warn("skip invalid method identifier: " + methodIdentifier + " for class: " + this.getClass().getName() + ", requestId: " + requestId +", parameters: " + parameters);
		}
		
		return null;
	}
}
