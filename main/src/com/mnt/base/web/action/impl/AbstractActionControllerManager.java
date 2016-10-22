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

package com.mnt.base.web.action.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.util.CommonUtil;
import com.mnt.base.web.action.AccessChecker;
import com.mnt.base.web.action.ActionController;
import com.mnt.base.web.action.annotation.ACMethod;
import com.mnt.base.web.action.annotation.ACParam;
import com.mnt.base.web.action.annotation.ACPath;
import com.mnt.base.web.action.annotation.ACWebHandler;
import com.mnt.base.web.action.impl.AbstractActionControllerManager.MethodInfoHolder.ResourceInfoHolder;



/**
 * 
 * @author Peyton Peng
 * @date 2012-3-22
 *
 *
 */
public abstract class AbstractActionControllerManager {
	
	private final Log log = LogFactory.getLog(getClass());
	
	protected String contextPath;
	protected int lenContxtPath;
	
	protected AbstractActionControllerManager(String contextPath) {
		this.contextPath = contextPath;
		this.lenContxtPath = this.contextPath.length();
	}
	
	protected class MethodInfoHolder {
		Method method;
		ACMethod acMethod;
		Map<String, Class<?>> paramsMap;
		Map<String, Map<String, Field>> beanFieldDefMap;
		
		protected class ResourceInfoHolder extends MethodInfoHolder {
			String resourceDef;
			Map<String, Integer> paramsIndexMap = new LinkedHashMap<String, Integer>();
			Pattern pattern;
			boolean isDefault;
			
			final Pattern RESOURCE_PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");
			
			public ResourceInfoHolder(String resourceDef) {
				this.resourceDef = resourceDef;
				isDefault = ACMethod.DEFAULT_RESOURCE_DEF.equals(this.resourceDef);
				this.method = MethodInfoHolder.this.method;
				this.acMethod = MethodInfoHolder.this.acMethod;
				this.paramsMap = MethodInfoHolder.this.paramsMap;
			}

			boolean parse() {
				Matcher matcher = RESOURCE_PARAM_PATTERN.matcher(resourceDef);
				
				StringBuffer buff = new StringBuffer();
				int paramIndex = 1;
				while(matcher.find()) {
					paramsIndexMap.put(matcher.group(1), paramIndex++);
					matcher.appendReplacement(buff, "([^\\/]+)");
				}
				
				if(paramIndex == 1) {
					buff.append(resourceDef);
				}
				
				if(buff.charAt(buff.length() - 1) != '/') {
					buff.append("/?");
				}
				
				pattern = Pattern.compile(new StringBuilder("^").append(buff.toString().replace("/", "\\/")).append("$").toString());
				return true;
			}
		}
	}
	
	protected class SimpleControllerWrapper extends ActionController {
		
		private Object controller;
		public SimpleControllerWrapper(Object controller) {
			this.controller = controller;
		}
		
		@Override
		public Object handleRequest(String method,
				Map<String, Object> parameters) {
			Map<String, Object> resultMap = new HashMap<String, Object>();
			resultMap.put(K_RESULT, ActionController.INVALID_REQUEST);
			return resultMap;
		}
		
		@Override
		public Object getController() {
			return controller;
		}

		@Override
		public String path() {
			return null;
		}
	}
	
	protected Map<String, ActionController> actionControllerMap = new HashMap<String, ActionController>();
	protected Map<ActionController, Map<String, MethodInfoHolder>> acMethodsMap = new HashMap<ActionController, Map<String, MethodInfoHolder>>();
	protected Map<ActionController, List<ResourceInfoHolder>> acResourcesMap = new HashMap<ActionController, List<ResourceInfoHolder>>();
	protected String defaultActionController;
	protected AccessChecker accessChecker;
	
	public void setSimpleControllers(Collection<Object> controllers) {
		if(!CommonUtil.isEmpty(controllers)) {
			Collection<ActionController> scs = new ArrayList<ActionController>();
			for(Object controller : controllers) {
				scs.add(new SimpleControllerWrapper(controller));
			}
			
			setControllers(scs);
		}
	}

	public void setControllers(Collection<ActionController> controllers) {
		if(!CommonUtil.isEmpty(controllers)){
			for(ActionController ac : controllers){
				
				Class<?> acClazz = ac.getController().getClass();
				
				if(CommonUtil.isEmpty(ac.path())){
					ACPath acPath = acClazz.getAnnotation(ACPath.class);
					ACWebHandler acWebHandler = acClazz.getAnnotation(ACWebHandler.class);
					
					if(acWebHandler != null) {
						ac.setWebHandler(acWebHandler.value());
					}
					
					if(acPath == null && acClazz.getInterfaces().length > 0) {
						
						Class<?>[] clazzs = acClazz.getInterfaces();
						
						for(Class<?> clazz : clazzs) {
							acClazz = clazz;
							acPath = acClazz.getAnnotation(ACPath.class);
							
							if(acPath != null) {
								acWebHandler = acClazz.getAnnotation(ACWebHandler.class);
								if(acWebHandler != null) {
									ac.setWebHandler(acWebHandler.value());
								}
								
								break;
							}
						}
					}
					
					if(acPath != null) {
						log.info(new StringBuilder("Attach the action controller for path: ").append(acPath.value()).append(", implementation class: ").append(ac.getController().getClass().getName()));
						actionControllerMap.put(acPath.value(), ac);
					} else {
						log.error("Skip the invalid action controller, the path of the action controller need to be specified, class: " + ac.getClass().getName());
						continue;
					}
				}else{
					log.info(new StringBuilder("Attach the action controller for path: ").append(ac.path()).append(", implementation class: ").append(ac.getClass().getName()));
					actionControllerMap.put(ac.path(), ac);
				}
				
				Map<String, MethodInfoHolder> acMs = new HashMap<String, MethodInfoHolder>();
				List<ResourceInfoHolder> acRs = new ArrayList<ResourceInfoHolder>();
				Method[] methods = acClazz.getDeclaredMethods();
				
				ACMethod acMethod;
				MethodInfoHolder mih;
				ResourceInfoHolder rih;
				
				for(Method m : methods) {
					acMethod = m.getAnnotation(ACMethod.class);
					
					if(acMethod != null) {
						
						mih = new MethodInfoHolder();
						mih.method = m;
						mih.method.setAccessible(true);
						mih.acMethod = acMethod;
						mih.paramsMap = new LinkedHashMap<String, Class<?>>();
						
						Class<?>[] pts = m.getParameterTypes();
						
						if(pts.length > 0) {
							
							Annotation[][] pass = m.getParameterAnnotations();
							
							Annotation[] pas;
							
							ACParam acParam;
							
							// skip the first two parameters: parameterMap, responseMap
							for(int i = 0; i < pass.length; i++) {
								pas = pass[i];
								
								if(pas.length != 1 || !(pas[0] instanceof ACParam)) {
									log.warn("no corresponding ACParam specified for actioncontroller: " + acClazz.getName() + " method: " + m.getName() + " parameter at index: " + i + ".");
									
									mih.paramsMap.put(ACParam.NULL_PREFIX + i, pts[i]);
									continue;
								}
								
								acParam = (ACParam)(pas[0]);
								
								if(acParam.beanPrefix()) {
									String beanFieldIndexKey = ACParam.BEAN_PREFIX + i;
									mih.paramsMap.put(beanFieldIndexKey, pts[i]);
									if(mih.beanFieldDefMap == null) {
										mih.beanFieldDefMap = new LinkedHashMap<String, Map<String, Field>>();
									}
									
									Class<?> paramClass = pts[i];
									Field[] fields = paramClass.getDeclaredFields();
									Map<String, Field> beanFieldDefMap = new LinkedHashMap<String, Field>();
									for(Field field : fields) {
										field.setAccessible(true);
										beanFieldDefMap.put(new StringBuilder(acParam.value()).append(".").append(field.getName()).toString(), field);
									}
									
									mih.beanFieldDefMap.put(beanFieldIndexKey, beanFieldDefMap);
								} else {
									mih.paramsMap.put(acParam.value(), pts[i]);
								}
							}
						}
						
						if(!CommonUtil.isEmpty(acMethod.resource())) {
							String resourceDef = acMethod.resource();
							rih = mih.new ResourceInfoHolder(resourceDef);
							if(rih.parse()) {
								acRs.add(rih);
							}
						}
						
						acMs.put(acMethod.type(), mih);
					}
				}
				
				if(!acMs.isEmpty()) {
					acMethodsMap.put(ac, acMs);
				}
				
				if(!acRs.isEmpty()) {
					acResourcesMap.put(ac, acRs);
				}
			}
		}
	}
	
	protected ActionController getRequestController(String[] secondLevelPaths) {
		ActionController ac = actionControllerMap.get(secondLevelPaths[0]);
		
		if(ac == null && !CommonUtil.isEmpty(defaultActionController)) {
			ac = actionControllerMap.get(defaultActionController);
		}
		
		return ac;
	}
	
	protected Object processRequest(String requestUri, String method, Map<String, Object> parameters) {
		String[] secondLevelPaths = detectSecondLevelPath(requestUri);
		ActionController ac = getRequestController(secondLevelPaths);
		return processRequest(ac, secondLevelPaths, requestUri, method, parameters);
	}

	protected Object processRequest(ActionController actionController, String[] secondLevelPaths, String requestUri, String method, Map<String, Object> parameters) {
		if(log.isDebugEnabled()){
			log.debug("Request: " + requestUri + ", method: " + method + ", parameters: " + parameters);
		}
		
		Object responseHolder = null;
		
		if(actionController != null) {
			// check if exists the type first
			String type = ActionController.getParameter(parameters, ActionController.K_TYPE);
			
			MethodInfoHolder mih = null;
			
			if(CommonUtil.isEmpty(type)) {
				
				boolean defaultFlag = false;
				String resValue = null;
				if(secondLevelPaths.length == 2) {
					resValue = secondLevelPaths[1];
				}
				
				if(CommonUtil.isEmpty(resValue)) {
					resValue = ACMethod.DEFAULT_RESOURCE_DEF;
					defaultFlag = true;
				}
				
				if(true) {
					List<ResourceInfoHolder> rihs = acResourcesMap.get(actionController);
					if(rihs != null) {
						for(ResourceInfoHolder rih : rihs) {
							// default only match the default 
							if(defaultFlag) {
								if(rih.isDefault) {
									mih = rih;
									break;
								} else {
									continue;
								}
							}
							
							Matcher matcher = rih.pattern.matcher(resValue);
							if(matcher.matches()) {
								for(String key : rih.paramsIndexMap.keySet()) {
									parameters.put(key, matcher.group(rih.paramsIndexMap.get(key)));
								}
								
								mih = rih;
								break;
							}
						}
					}
				}
			} else {
				Map<String, MethodInfoHolder> mihs = acMethodsMap.get(actionController);
				if(mihs != null) {
					mih = mihs.get(type);
				}
			}
				
			if(mih != null) {
				
				ACMethod acMethod = mih.acMethod;
				
				if(!acMethod.accessCheck() || accessChecker.accessable(parameters, acMethod.checkLevel(), acMethod.accessRole())) {
					
					List<Object> paramVals = new ArrayList<Object>();
					
					Map<String, Class<?>> paramsSpec = mih.paramsMap;
					
					if(paramsSpec != null) {
						
						for(String key : paramsSpec.keySet()) {
							if(ACParam.ALL_PARAMS.equals(key)) {
								paramVals.add(parameters);
							} else if (ACParam.RESPONSE_MAP.equals(key)) {
								responseHolder = new LinkedHashMap<String, Object>();
								paramVals.add(responseHolder);
							} else if (key.startsWith(ACParam.NULL_PREFIX)) {
								paramVals.add(null);
							} else if (key.startsWith(ACParam.BEAN_PREFIX)) {
								Object beanObj = createBeanObj(mih.beanFieldDefMap.get(key), paramsSpec.get(key), parameters);
								paramVals.add(beanObj);
							} else {
								paramVals.add(ActionController.getParameterValue(parameters, key, paramsSpec.get(key)));
							}
						}
					}
					
					Method mtd = mih.method;
					
					try {
						
						if(mtd.getReturnType() == Void.class) {
							mtd.invoke(actionController.getController(), paramVals.toArray());
						} else {
							Object result = mtd.invoke(actionController.getController(), paramVals.toArray());
							responseHolder = (responseHolder == null) ? result : responseHolder;
						}
					} catch (Exception e) {
						log.error("Error when process the request by ActionController", e);
						
						Map<String, Object> resultMap = new HashMap<String, Object>();
						resultMap.put(ActionController.K_RESULT, ActionController.SYSTEM_INTERNAL_ERROR);
						responseHolder = resultMap;
						
					} finally {
						paramVals.clear();
					}
				} else {
					Map<String, Object> resultMap = new HashMap<String, Object>();
					resultMap.put(ActionController.K_RESULT, ActionController.ACCESS_NO_PRIVILEGE);
					responseHolder = resultMap;
				}
			} else {
				try{
					responseHolder = actionController.handleRequest(method, parameters);
				}catch(Exception e){
					log.error("Error when process the request by ActionController", e);
				}
			}
		}else{
			log.warn("Invalid second level path access: " + "Request: " + requestUri + ", method: " + method + ", parameters: " + parameters);
		}
		
		return responseHolder;
	}
	
	private Object createBeanObj(Map<String, Field> beanFieldDefMap, Class<?> beanClass, Map<String, Object> parameters) {
		
		Object beanObj = null;
		try {
			beanObj = beanClass.newInstance();
		} catch (Exception e) {
			log.error("error while create bean object for beanClass: " + beanClass.getName());
		}
		
		if(beanObj != null) {
			//ActionController.getParameterValue(parameters, key, paramsSpec.get(key))
			
			Object fieldVal;
			Field field;
			for(String fieldRef : beanFieldDefMap.keySet()) {
				if(parameters.containsKey(fieldRef)) {
					field = beanFieldDefMap.get(fieldRef);
					fieldVal = ActionController.getParameterValue(parameters, fieldRef, field.getType());
					if(fieldVal != null) {
						try {
							field.set(beanObj, fieldVal);
						} catch (Exception e) {
							log.error("error while set the bean field value: " + fieldVal + ", beanObj: " + beanObj + ", field: " + field.getName());
						}
					}
				}
			}
		}
		
		return beanObj;
	}

	protected String[] detectSecondLevelPath(String requestUri){
		String[] secondLevelPaths = null;
		
		if(requestUri.startsWith(contextPath) && requestUri.length() > lenContxtPath){
			String secondLevelPath = requestUri.substring(contextPath.length() + 1); // context value: "/<contextpath>", need to remove the value "/<contextpath>/"
			secondLevelPaths = secondLevelPath.split("\\/", 2);
		}
		
		return secondLevelPaths;
	}

	public String getDefaultActionController() {
		return defaultActionController;
	}

	public void setDefaultActionController(String defaultActionController) {
		this.defaultActionController = defaultActionController;
	}

	public AccessChecker getAccessChecker() {
		return accessChecker;
	}

	public void setAccessChecker(AccessChecker accessChecker) {
		this.accessChecker = accessChecker;
	}
}
