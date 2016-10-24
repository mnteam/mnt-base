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

package com.mnt.base.json;

/**
 * json support class
 * @author Peng Peng
 *
 */
interface JsonSupport {
	
	String CLASS_FIELD = "__class";
	String COLLECTION_CLASS_PFRIEX = "__collection_";
	String MAP_CLASS_PFRIEX = "__map_";
	
	String VALUE = "value";
	
	String METHOD_GET_PREFIX = "get";
	String METHOD_SET_PREFIX = "set";
	String METHOD_IS_PREFIX  = "is";
	int GET_PREFIX_LEN = METHOD_GET_PREFIX.length();
	int GET_PREFIX_LEN_1 = GET_PREFIX_LEN + 1;
	
	int IS_PREFIX_LEN = METHOD_IS_PREFIX.length();
	int IS_PREFIX_LEN_1 = IS_PREFIX_LEN + 1;
	
	Object[] EMTPY_OBJ_ARR = new Object[]{};
	
	char DOT   = '.';
	char COLON = ':';
	char COMMA = ',';
	char OBJ_START = '{';
	char OBJ_END = '}';
	char ARR_START = '[';
	char ARR_END = ']';
	char SPACE = ' ';
	char BACKSLASH = '\\';
	char S_QUOT = '\'';
	char D_QUOT = '"';
	
	String S_COLON = ":";
	String S_COMMA = ",";
	String S_OBJ_END = "}";
	String S_ARR_END = "]";
	
	String QUOT_STR = "\"";
	String QUOT_STR_REP = "\\\"";
	
	String BACKSLASH_STR = "\\";
	String BACKSLASH_STR_REP = "\\\\";
	
	String UNICODE_START = "\\u";
	
	String STATIC_VALUE_NULL = "null";
	int VALUE_NULL_LEN 		 = STATIC_VALUE_NULL.length();
	String STATIC_VALUE_TRUE = "true";
	String STATIC_VALUE_FALSE = "false";
	String STATIC_VALUE_UNDEFINED = "undefined";
	
	Number DEFAULT_NUMBER_VALUE = 0; 
}