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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Server runtime bean management context wrapper.
 * 
 * @author Peng Peng
 * @date 2013-02-17
 *
 *
 */
public abstract class BeanContext {
	
	private static final Log log = LogFactory.getLog(BeanContext.class);

	private static BeanContext beanContext;
	
	/**
	 * <pre>
	 * Get BeanContext instance.
	 * 
	 * It would return null while the method putBeanContext does not invoke before it be called. 
	 * </pre>
	 * 
	 * @return BeanContext
	 */
	public static BeanContext getInstance() {
		return beanContext;
	}
	
	/**
	 * Put the BeanContext instance.
	 * 
	 * @param beanContext
	 */
	public synchronized static void putInstance(BeanContext beanContext) {
		
		if(BeanContext.beanContext != null) {
			log.warn("Try to overwrite the static variable beanContext: " + BeanContext.beanContext + " by new: " + beanContext);
		}
		
		BeanContext.beanContext = beanContext;
	}
	
	/**
	 * Get bean with the addressed beanId and class type, return null while the corresponding class instance not found.
	 * 
	 * @param beanId
	 * @param clazz
	 * @return
	 */
	public abstract <T> T getBean(String beanId, Class<T> clazz);
	
	/**
	 * Get bean with the addressed beanId, return null while the corresponding class instance not found.
	 * 
	 * @param beanId
	 * @return
	 */
	public abstract Object getBean(String beanId);
}
