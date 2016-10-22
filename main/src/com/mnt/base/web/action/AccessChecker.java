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

import com.mnt.base.web.action.annotation.ACMethod;

public interface AccessChecker {
	
	int CK_LOGIN				= ACMethod.CK_LOGIN;
	int CK_PARAM_VALIDATION 	= ACMethod.CK_PARAM_VALIDATION;

	/**
	 * check if the method is accessable by current request parameters, the corresponding method should specify the 
	 * ACMethod(accessCheck=true)
	 * 
	 * @param parameters
	 * @param checkLevel
	 * @param accessRole
	 * @return
	 */
	boolean accessable(Map<String, Object> parameters, int checkLevel, int accessRole) ;
}
