package com.mnt.base.das;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class DataSourceFactory extends DBFactory {
	
	protected DataSource dataSource;
	
	
	protected DataSourceFactory() {
		this(null);
	}
	
	protected DataSourceFactory(DataSource dataSource) {
		super(null);
		this.dataSource = dataSource;
	}

	private static Log log = LogFactory.getLog(DBFactory.class);
	
	private static ThreadLocal<TransactionOwner> toHolder = new ThreadLocal<TransactionOwner>();
	
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
		
		try {
			connection = dataSource.getConnection();
		} catch (SQLException e) {
			log.error("error while retrieve the db connection from datasource.", e);
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
				
				try {
					((Connection)o).close();
				} catch (SQLException e) {
					log.error("error while close the connection resource.", e);
					//throw new RuntimeException("Error while closing the connection resource.", e);
				}
			}else if(o instanceof PreparedStatement){
				try {
					((PreparedStatement) o).close();
				} catch (SQLException e) {
					log.error("Error while closing the PreparedStatement resource.", e);
					//throw new RuntimeException("Error while closing the PreparedStatement resource.", e);
				}
			}else if(o instanceof ResultSet){
				try {
					((ResultSet) o).close();
				} catch (SQLException e) {
					log.error("Error while closing the ResultSet resource.", e);
					//throw new RuntimeException("Error while closing the ResultSet resource.", e);
				}
			}else if(o instanceof Statement){
				try {
					((Statement) o).close();
				} catch (SQLException e) {
					log.error("Error while closing the Statement resource.", e);
					//throw new RuntimeException("Error while closing the Statement resource.", e);
				}
			}else{
				log.error("Unknow db resource type: " + o.getClass().getName());
				//throw new RuntimeException("Unknow db resource type: " + o.getClass().getName());
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

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}
