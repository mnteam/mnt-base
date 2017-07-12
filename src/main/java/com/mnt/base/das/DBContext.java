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

package com.mnt.base.das;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DB context impl for JDBC.
 * @author Peng Peng
 *
 */
public class DBContext implements IContext {
	
	protected DBFactory dbFactory;
	protected int queryTimeout;
	
	public DBContext() {
		this(false);
	}
	
	public DBContext(boolean skipDefault) {
		if(!skipDefault) {
			dbFactory = DBFactory.getDBFactory(DBFactory.FactoryType.RELATION_DB);
			queryTimeout = dbFactory.getQueryTimeout();
		}
	}
	
	public DBFactory getDBFactory(){
		return dbFactory;
	}

	@Override
	public boolean save(String sql, List<Object> params) {
		boolean result = false;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				result = prepStmt.executeUpdate() == 1;
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.save(%s, %s).", sql, params), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param sql
	 * @param params
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <PK> PK saveAndReturnGeneratedKey(String sql, List<Object> params) {

		PK pk = null;
		Connection con = getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			try {
				prepStmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				if(prepStmt.executeUpdate() == 1) { 
					ResultSet rs = prepStmt.getGeneratedKeys();
					
					if(rs.next()) {
						pk = (PK)rs.getObject(1);
					}
					
					close(rs);
				}
			} catch (SQLException e) {
				throw new RuntimeException("Error when invoke DBContext.save(sql, params).", e);
			}finally{
				if(prepStmt != null){
					close(prepStmt);
				}
				
				if(con != null){
					close(con);
				}
			}
		}
		
		return pk;
	}
	
	@Override
	public boolean bulkSave(String sql, List<List<Object>> batchParams) {
		boolean result = false;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			
			if(!TransactionManager.isTransactional()) {
				try {
					con.setAutoCommit(false);
				} catch (SQLException e) {
					throw new RuntimeException(String.format("Error when invoke DBContext.bulkSave(%s, params) where initial transaction.", sql), e);
				}
			}
			
			PreparedStatement prepStmt = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(batchParams != null && batchParams.size() > 0){
					
					for(List<Object> params : batchParams){
						if(params != null && params.size() > 0){
							for(int i = 0; i < params.size(); i++){
								prepStmt.setObject(i + 1, params.get(i));
							}
							prepStmt.addBatch();
						}
					}
				}
				
				result = prepStmt.executeBatch().length == batchParams.size();
				
				if(!TransactionManager.isTransactional()) {
					con.commit();
				}
			} catch (SQLException e) {
				if(!TransactionManager.isTransactional()) {
					try {
						con.rollback();
					} catch (SQLException e1) {
						throw new RuntimeException(String.format("Error when invoke DBContext.bulkSave(%s, params) where rollback.", sql), e);
					}
				}
				throw new RuntimeException(String.format("Error when invoke DBContext.bulkSave(%s, params).", sql), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				
				if(con != null) {
					if(!TransactionManager.isTransactional()) {
				
						try {
							con.setAutoCommit(true);
						} catch (SQLException e) {
							throw new RuntimeException(String.format("Error when invoke DBContext.bulkSave(%s, params) where commit.", sql), e);
						}
					}
				
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}
	
	@Override
	public int[] bulkUpdate(String sql, List<List<Object>> batchParams) {
		int[] result = null;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			
			if(!TransactionManager.isTransactional()) {
				try {
					con.setAutoCommit(false);
				} catch (SQLException e) {
					throw new RuntimeException(String.format("Error when invoke DBContext.bulkUpdate(%s, params) where initial transaction.", sql), e);
				}
			}
			
			PreparedStatement prepStmt = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(batchParams != null && batchParams.size() > 0){
					for(List<Object> params : batchParams){
						if(params != null && params.size() > 0){
							for(int i = 0; i < params.size(); i++){
								prepStmt.setObject(i + 1, params.get(i));
							}
							prepStmt.addBatch();
						}
					}
				}
				
				result = prepStmt.executeBatch();
				if(!TransactionManager.isTransactional()) {
					con.commit();
				}
			} catch (SQLException e) {
				if(!TransactionManager.isTransactional()) {
					try {
						con.rollback();
					} catch (SQLException e1) {
						throw new RuntimeException(String.format("Error when invoke DBContext.bulkUpdate(%s, params) while rollback.", sql), e);
					}
				}
				throw new RuntimeException(String.format("Error when invoke DBContext.bulkUpdate(%s, params).", sql), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					
					if(!TransactionManager.isTransactional()) {
						try {
							con.setAutoCommit(true);
						} catch (SQLException e) {
							throw new RuntimeException(String.format("Error when invoke DBContext.bulkUpdate(%s, params) while commit.", sql), e);
						}
					}
					
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}
	
	@Override
	public int count(String sql, List<Object> params){
		int result = 0;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			ResultSet rs = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				rs = prepStmt.executeQuery();
				
				if(rs.next()){
					result = rs.getInt(1);
				}
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.count(%s, %s).", sql, params), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}

	@Override
	public boolean exists(String sql, List<Object> params){
		boolean result = false;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			ResultSet rs = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				rs = prepStmt.executeQuery();
				
				if(rs.next()){
					result = rs.getInt(1) > 0;
				}
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.exists(%s, %s).", sql, params), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}
	
	@Override
	public List<Map<String, Object>> query(String sql, List<Object> params,
			List<String> resultKeys) {
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			ResultSet rs = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				rs = prepStmt.executeQuery();
				
				Map<String, Object> result = null;
				while(rs.next()){
					
					result = new LinkedHashMap<String, Object>();
					for(String key : resultKeys){
						result.put(key, rs.getObject(key));
					}
					
					results.add(result);
				}
				
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.query(%s, %s, %s).", sql, params, resultKeys), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return results;
	}
	
	@Override
	public List<Map<String, Object>> query(String sql, List<Object> params) {
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			ResultSet rs = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				rs = prepStmt.executeQuery();
				
				ResultSetMetaData metaData = rs.getMetaData();
				
				List<String> resultKeys = new ArrayList<String>();
				
				for(int x = 1; x <= metaData.getColumnCount(); x++ ){
					resultKeys.add(metaData.getColumnLabel(x));
				}
				
				Map<String, Object> result = null;
				while(rs.next()){
					
					result = new LinkedHashMap<String, Object>();
					for(String key : resultKeys){
						result.put(key, rs.getObject(key));
					}
					
					results.add(result);
				}
				
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.query(%s, %s).", sql, params), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return results;
	}
	
	@Override
	public List<Map<String, Object>> query(String sql, List<Object> params,
			Map<String, String> resultKeyMap) {
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			ResultSet rs = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				rs = prepStmt.executeQuery();
				
				Map<String, Object> result = null;
				while(rs.next()){
					
					result = new LinkedHashMap<String, Object>();
					for(String key : resultKeyMap.keySet()){
						result.put(resultKeyMap.get(key), rs.getObject(key));
					}
					
					results.add(result);
				}
				
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.query(%s, %s, %s).", sql, params, resultKeyMap), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return results;
	}
	
	@Override
	public int update(String sql, List<Object> params) {
		int result = 0;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				result = prepStmt.executeUpdate();
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.update(%s, %s).", sql, params), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}

	

	@Override
	public int delete(String sql, List<Object> params) {
		int result = 0;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				result = prepStmt.executeUpdate();
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.delete(%s, %s).", sql, params), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}

	@Override
	public boolean execute(String sql) {
		boolean result = false;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			Statement statment = null;
			try {
				statment = con.createStatement();
				populateStatement(statment);
				result = statment.execute(sql);
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.execute(%s).", sql), e);
			}finally{
				if(statment != null){
					getDBFactory().close(statment);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}

	@Override
	public Map<String, Object> get(String sql, List<Object> params, List<String> resultKeys) {
		Map<String, Object> result = null;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			ResultSet rs = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				rs = prepStmt.executeQuery();
				
				if(rs.next()){
					
					result = new HashMap<String, Object>();
					for(String key : resultKeys){
						result.put(key, rs.getObject(key));
					}
				}
				
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.get(%s, %s, %s).", sql, params, resultKeys), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}
	
	@Override
	public Map<String, Object> get(String sql, List<Object> params) {
		Map<String, Object> result = null;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			ResultSet rs = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				rs = prepStmt.executeQuery();
				
				ResultSetMetaData metaData = rs.getMetaData();
				
				List<String> resultKeys = new ArrayList<String>();
				
				for(int x = 1; x <= metaData.getColumnCount(); x++ ){
					resultKeys.add(metaData.getColumnLabel(x));
				}
				
				if(rs.next()){
					
					result = new HashMap<String, Object>();
					for(String key : resultKeys){
						result.put(key, rs.getObject(key));
					}
				}
				
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.get(%s, %s).", sql, params), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}
	
	@Override
	public Map<String, Object> get(String sql, List<Object> params, Map<String, String> resultKeyMap) {
		Map<String, Object> result = null;
		Connection con = getDBFactory().getConnection();
		
		if(con != null){
			PreparedStatement prepStmt = null;
			ResultSet rs = null;
			try {
				prepStmt = con.prepareStatement(sql);
				populateStatement(prepStmt);
				
				if(params != null && params.size() > 0){
					for(int i = 0; i < params.size(); i++){
						prepStmt.setObject(i + 1, params.get(i));
					}
				}
				
				rs = prepStmt.executeQuery();
				
				if(rs.next()){
					
					result = new HashMap<String, Object>();
					for(String key : resultKeyMap.keySet()){
						result.put(resultKeyMap.get(key), rs.getObject(key));
					}
				}
				
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Error when invoke DBContext.get(%s, %s, %s).", sql, params, resultKeyMap), e);
			}finally{
				if(prepStmt != null){
					getDBFactory().close(prepStmt);
				}
				
				if(con != null){
					getDBFactory().close(con);
				}
			}
		}
		
		return result;
	}
	
	protected void populateStatement(Statement stat) throws SQLException {
		
		if(queryTimeout > 0) {
			stat.setQueryTimeout(queryTimeout);
		}
	}

	@Override
	public Connection getConnection() {
		return getDBFactory().getConnection();
	}

	@Override
	public void close(Object resource) {
		getDBFactory().close(resource);
	}
}