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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.core.util.BaseConfiguration;



/**
 * provide the way to retrieve the DB connection.
 * @author Peng Peng
 *
 */
public abstract class DBFactory {
	
	protected static final String DB_USER       = "jdbc.username";
	protected static final String DB_PASSWORD   = "jdbc.password";
	protected static final String DB_URL        = "jdbc.url";
	protected static final String DB_DRIVER     = "jdbc.driver";
	protected static final String DB_POOL_SIZE  = "jdbc.maxPoolSize";
	
	private BlockingQueue<Connection> connPool = new LinkedBlockingQueue<Connection>();
	private BlockingQueue<Connection> usingConnPool = new LinkedBlockingQueue<Connection>();
	
	private Object connCreationLock = new Object();
	
	private int maxPoolSize = 10;
	
	protected Properties prop = new Properties();
	
	private static Log log = LogFactory.getLog(DBFactory.class);
	
	protected static ThreadLocal<TransactionOwner> toHolder = new ThreadLocal<TransactionOwner>();
	
	enum FactoryType{
		RELATION_DB,
		FILE_DB
	}

	protected DBFactory(String dbConf){
		if(dbConf != null) {
			load(dbConf);
		}
	}
	
	public static DBFactory getDBFactory(FactoryType factoryType){
		if(factoryType == FactoryType.RELATION_DB){
			return RDBFactory.getInstance();
		}else{
			throw new RuntimeException("Unsupport DBFactory Type.");
		}
	}
	
	protected void load(String dbConf){
		InputStream in = BaseConfiguration.getRelativeFileStream(dbConf);
		
		if(in != null){
			try {
				prop.load(in);
			} catch (IOException e) {
				log.error("Error when loading properties from db conf file.", e);
				throw new RuntimeException("Error when loading properties from db conf file.", e);
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					log.error("error while close the dbconf file: " + dbConf, e);
				}
			}
			
			try {
				Class.forName(prop.getProperty(DB_DRIVER));
			} catch (ClassNotFoundException e) {
				log.error("Error when loading db driver: " + prop.getProperty(DB_DRIVER), e);
				throw new RuntimeException("Error when loading db driver: " + prop.getProperty(DB_DRIVER), e);
			}
			
			try{
				maxPoolSize = Integer.parseInt(prop.getProperty(DB_POOL_SIZE));
			}catch(Throwable e){
				log.debug("Not set the db_pool_size, use the default value 10.");
				maxPoolSize = 10;
			}
			
		}else{
			log.error("Could not found db config file: " + dbConf);
			throw new RuntimeException("Could not found db config file: " + dbConf);
		}
	}
	
	public Connection getConnection(){
		
		Connection conn;
		
		TransactionOwner to = toHolder.get();
		
		if(to != null) {
			conn = to.connection;
		} else {
			conn = retrieveConnection();
		}
		
		return conn;
	}
	
	private Connection retrieveConnection() {
		
		Connection connection = null;
		
		while(connPool.size() > 0 || usingConnPool.size() >= maxPoolSize) {
			connection = connPool.poll();
			
			if(connection != null) {
				try {
					if(connection.isValid(0)){
						
						usingConnPool.add(connection);
						
						return connection;
					}
				} catch (SQLException e) {
					try {
						connection.close();
					} catch (SQLException e1) {
						// ignore
					}
				}
			} else {
				Iterator<Connection> conns = usingConnPool.iterator();
				Connection conn;
				while(conns.hasNext()) {
					conn = conns.next();
							
					try {
						if(conn.isClosed()){
							usingConnPool.remove(conn);
						}
					} catch (SQLException e) {
						
						try{
							conn.close();
						}catch(SQLException e1){
							// skip it
						}
						
						usingConnPool.remove(conn);
					}
						
					if(usingConnPool.size() < maxPoolSize){
						break;
					}
				}
				
				if(usingConnPool.size() >= maxPoolSize) {
					synchronized (connPool) {
						try {
							connPool.wait(1000);
						} catch (InterruptedException e) {
							log.error("error while wait for the connection.", e);
						}
					}
				}
			}
		}
		
		synchronized (connCreationLock) {
			if(usingConnPool.size() < maxPoolSize){
				try {
					connection = DriverManager.getConnection(prop.getProperty(DB_URL), 
															prop.getProperty(DB_USER), 
															prop.getProperty(DB_PASSWORD));
					
					usingConnPool.add(connection);
				} catch (SQLException e) {
					log.error("Error when construct the db connection.", e);
					throw new RuntimeException("Error when construct the db connection.", e);
				}
			}
		}
		
		if(connection == null) {
			synchronized (connPool) {
				try {
					connPool.wait(1000);
				} catch (InterruptedException e) {
					log.error("error while wait for the connection.", e);
				}
			}
			
			return getConnection();
		}
			
		return connection;
	}
	
	public void close(Object o){
		if(o != null){
			
			if(o instanceof Connection){
				
				TransactionOwner to = toHolder.get();
				if(to != null && !to.closeable()) {
					return ;
				}
				
				synchronized (connPool) {
					connPool.add((Connection)o);
					usingConnPool.remove(o);
					
					connPool.notify();
				}
			}else if(o instanceof PreparedStatement){
				try {
					((PreparedStatement) o).close();
				} catch (SQLException e) {
					log.error("Error while closing the PreparedStatement resource.", e);
					throw new RuntimeException("Error while closing the PreparedStatement resource.", e);
				}
			}else if(o instanceof ResultSet){
				try {
					((ResultSet) o).close();
				} catch (SQLException e) {
					log.error("Error while closing the ResultSet resource.", e);
					throw new RuntimeException("Error while closing the ResultSet resource.", e);
				}
			}else if(o instanceof Statement){
				try {
					((Statement) o).close();
				} catch (SQLException e) {
					log.error("Error while closing the Statement resource.", e);
					throw new RuntimeException("Error while closing the Statement resource.", e);
				}
			}else{
				log.error("Unknow db resource type: " + o.getClass().getName());
				throw new RuntimeException("Unknow db resource type: " + o.getClass().getName());
			}
		}
	}

	public void beginTransaction() {
		
		
		TransactionOwner to = toHolder.get();
		
		if(to == null) {
			Connection conn = retrieveConnection();
			try {
				conn.setAutoCommit(false);
			} catch (SQLException e) {
				log.error("fail to set connection auto commit as false.", e);
				throw new RuntimeException("Begin transaction error: fail to set connection auto commit as false.", e);
			}
			
			to = new TransactionOwner(conn);
			
			toHolder.set(to);
		} else {
			to.increaseDeep();
		}
		
		
	}

	public void endTransaction() {
		
		TransactionOwner to = toHolder.get();
		
		if(to.decressDeep()) {
			Connection conn = to.connection;
			
			try {
				conn.commit();
			} catch (SQLException e) {
				log.error("Fail to commit the transaction, auto rollback.", e);
				
				try {
					conn.rollback();
				} catch (SQLException e1) {
					log.error("Fail to rollback the transaction.", e1);
					throw new RuntimeException("End transaction error: fail to rollback the transaction.", e1);
				}
			}
			
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) {
				log.error("fail to set connection auto commit as true after the transacation.", e);
				throw new RuntimeException("end transaction error: fail to set connection auto commit as true.", e);
			}
			
			close(conn);
			
			toHolder.remove();
		}
	}

	public void rollback() {
		
		TransactionOwner to = toHolder.get();
		
		if(to != null) {
			try {
				to.connection.rollback();
			} catch (SQLException e) {
				log.error("Fail to rollback the transaction.", e);
				throw new RuntimeException("Transaction rollback error.", e);
			} finally {
				try {
					to.connection.setAutoCommit(true);
				} catch (SQLException e) {
					log.error("fail to set connection auto commit as true after the transacation.", e);
					throw new RuntimeException("end transaction error: fail to set connection auto commit as true.", e);
				}
			}
			
			to.deep = 0;
			
			close(to.connection);
			toHolder.remove();
		}
	}
	
	public boolean isTransactional() {
		return toHolder.get() != null;
	}
	
	private class TransactionOwner {
		Connection connection;
		int deep;
		
		TransactionOwner(Connection connection) {
			this(connection, 1);
		}



		TransactionOwner(Connection connection, int deep) {
			super();
			this.connection = connection;
			this.deep = deep;
		}
		
		void increaseDeep() {
			++ deep;
		}
		
		boolean decressDeep() {
			return --deep == 0;
		}
		
		public boolean closeable () {
			return deep == 0;
		}
	}
}