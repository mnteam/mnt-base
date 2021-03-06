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

package com.mnt.base.web.action;

import java.util.Map;

/**
 * 
 * @author Peng Peng
 * #date 2012-3-22
 *
 * define all action controller manager.
 *
 */
public interface ActionControllerManager extends ActionControllerManagerBase<ActionController> {
	/**
	 * Dispatch the request to second level path matched action controller.
	 * 
	 * @param requestUri
	 * @param method
	 * @param parameterMap
	 * @param responseHandler
	 */
	public void dispatchRequest(String requestUri, String method, Map<String, Object> parameterMap, ResponseHandler responseHandler);
}
