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

package com.mnt.core.das;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Define the data access service common methods.
 * @author Peng Peng
 *
 */

public interface IContext {

	/**
	 * save data with sql and params
	 * (support only for rdb context), while invoked in file db Context, it throw the UnsupportedOperationException
	 * 
	 * params number and order is corresponding with the value in sql replaced by "?" 
	 * 
	 * @param sql
	 * @param params
	 * @return
	 */
	boolean save(String sql, List<Object> params);
	
	
	/**
	 * update data with sql and params
	 * params number and order is corresponding with the value in sql replaced by "?" 
	 * 
	 * @param sql
	 * @param params
	 * @return
	 */
	int update(String sql, List<Object> params);
	
	/**
	 * query data with sql and params, the result keys defined the retrieving value set from result set.
	 * 
	 * result stored as a list, each record stored in map with key-value pair
	 * 
	 * @param sql
	 * @param params
	 * @param resultKeys
	 * @return
	 */
	List<Map<String, Object>> query(String sql, List<Object> params, List<String> resultKeys);
	
	/**
	 * query data with sql and params, the result keys defined the retrieving value set from result set.
	 * 
	 * result stored as a list, each record stored in map with key-value pair
	 * 
	 * @param sql
	 * @param params
	 * @param resultKeys
	 * @return
	 */
	List<Map<String, Object>> query(String sql, List<Object> params, Map<String, String> resultKeyMap);
	
	/**
	 * query data with sql and params, the result keys refer to the table metadata.
	 * 
	 * result stored as a list, each record stored in map with key-value pair
	 * 
	 * @param sql
	 * @param params
	 * @param resultKeys
	 * @return
	 */
	List<Map<String, Object>> query(String sql, List<Object> params);
	
	/**
	 * get the record with sql and params, it return the first record if exists. (if there is more than one record returns from db, just ignore the rest records)
	 * 
	 * @param sql
	 * @param params
	 * @param resultKeys
	 * @return
	 */
	Map<String, Object> get(String sql, List<Object> params, List<String> resultKeys);
	
	/**
	 * delete data with sql and params
	 * 
	 * @param sql
	 * @param params
	 * @return
	 */
	int delete(String sql, List<Object> params);
	
	/**
	 * execute sql, return true if the sql executed successfully.
	 * 
	 * @param sql
	 * @return
	 */
	boolean execute(String sql);

	/**
	 * count the data with sql, you'd better to providing sql like:
	 * 
	 * select count(1) from ...
	 * 
	 * @param sql
	 * @param params
	 * @return
	 */
	int count(String sql, List<Object> params);

	/**
	 * check if the data exists with sql and params
	 * return true if there is any data return.
	 * 
	 * You need to write the sql like:
	 * 
	 * select count(1) from ...
	 * 
	 * @param sql
	 * @param params
	 * @return
	 */
	boolean exists(String sql, List<Object> params);

	/**
	 * store multiple data(batch)
	 * (support only for rdb context), while invoked in file db Context, it throw the UnsupportedOperationException
	 * 
	 * @param storeData
	 * @param params
	 * @return
	 */
	boolean bulkSave(String storeData, List<List<Object>> params);
	
	/**
	 * bulk update by sql and parameters
	 * 
	 * @param sql
	 * @param params
	 * @return
	 */
	int[] bulkUpdate(String sql, List<List<Object>> params);
	
	/**
	 * Get the jdbc connection, for reuse the connection, you can use IContext.close(resource) to close the collection
	 * 
	 * @return
	 */
	Connection getConnection();
	
	/**
	 * close the opened resource, 
	 * e.g. 
	 * Connection
	 * PreparedStatement
	 * Statement
	 * ResultSet
	 * 
	 * if other object type passed in, throw RuntimeException.
	 * @param resource
	 */
	void close(Object resource);


	/**
	 * get the record map with specified sql, params and result key map
	 * @param sql
	 * @param params
	 * @param resultKeyMap
	 * @return
	 */
	Map<String, Object> get(String sql, List<Object> params, Map<String, String> resultKeyMap);

	/**
	 * get row data by sql and params.
	 * @param sql
	 * @param params
	 * @return
	 */
	Map<String, Object> get(String sql, List<Object> params);
}
