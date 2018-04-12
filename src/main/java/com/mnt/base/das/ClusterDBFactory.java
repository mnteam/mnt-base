package com.mnt.base.das;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.util.BaseConfiguration;
import com.mnt.base.util.CommonUtil;
import com.mnt.base.util.TimeUtil;




/**
 * provide the way to retrieve the DB connection.
 * @author Peng Peng
 *
 */
public abstract class ClusterDBFactory extends DBFactory {
	
	private static Log log = LogFactory.getLog(ClusterDBFactory.class);
	
	protected static final String CLUSTER_DB_DRIVERS    = "cluster.jdbc.drivers";
	protected static final String CLUSTER_DB_DRIVER     = "cluster.jdbc.driver";
	
	protected static final String CLUSTER_DB_URL        = "cluster.jdbc.url";
	protected static final String CLUSTER_DB_USER       = "cluster.jdbc.username";
	protected static final String CLUSTER_DB_PASSWORD   = "cluster.jdbc.password";
	protected static final String CLUSTER_DB_POOL_SIZE  = "cluster.jdbc.maxPoolSize";
	
	private class NodeDBFactory {
		
		String nodeName = ""; 
		Properties prop = new Properties();
		
		int maxPoolSize = 10;
		
		BlockingQueue<Connection> connPool = new LinkedBlockingQueue<Connection>();
		BlockingQueue<Connection> usingConnPool = new LinkedBlockingQueue<Connection>();
		
		Object connCreationLock = new Object();
		
		long nextCheckTs = 0;
		
		boolean available() {
			return System.currentTimeMillis() > nextCheckTs;
		}
		
		long waitTimestamp() {
			return System.currentTimeMillis() - nextCheckTs;
		}
		
		void setToWait() {
			nextCheckTs = System.currentTimeMillis() + TimeUtil.MINUTES_1;
		}
		
		void setConf(String confName, Object confValue) {
			
			if("maxPoolSize".equals(confName)) {
				this.maxPoolSize = CommonUtil.parseAsInt(confValue, this.maxPoolSize);
			}
			
			prop.put(new StringBuilder("jdbc.").append(confName), confValue);
		}

		@Override
		public String toString() {
			return "NodeDBFactory [nodeName=" + nodeName + ", prop=" + prop
					+ ", maxPoolSize=" + maxPoolSize + ", connPool=" + connPool
					+ ", usingConnPool=" + usingConnPool
					+ ", connCreationLock=" + connCreationLock + "]";
		}
	}
	
	private Map<String, NodeDBFactory> clusterNodes = new HashMap<String, NodeDBFactory>();
	private List<NodeDBFactory> nodeDBFactorys = new ArrayList<NodeDBFactory>();
	private AtomicLong loopIdx = new AtomicLong();
	
	private static ThreadLocal<TransactionOwner> toHolder = new ThreadLocal<TransactionOwner>();
	private static ThreadLocal<Integer> nodeRetrieveIdx = new ThreadLocal<Integer>();
	
	protected ClusterDBFactory(String dbConf){
		super(null);
		loadClusterConf(dbConf);
	}
	
	protected void loadClusterConf(String dbConf){
		InputStream in = BaseConfiguration.getRelativeFileStream(dbConf);
		Properties clusterProp = new Properties();
		if(in != null){
			try {
				clusterProp.load(in);
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
			
			boolean existsDrivers = false;
			if(clusterProp.containsKey(CLUSTER_DB_DRIVER)) {
				try {
					Class.forName(clusterProp.getProperty(CLUSTER_DB_DRIVER));
					existsDrivers = true;
				} catch (ClassNotFoundException e) {
					log.error("Error when loading db driver: " + clusterProp.getProperty(DB_DRIVER), e);
					throw new RuntimeException("Error when loading db driver: " + clusterProp.getProperty(DB_DRIVER), e);
				}
			}

			if(clusterProp.containsKey(CLUSTER_DB_DRIVERS)){
				String drivers = clusterProp.getProperty(CLUSTER_DB_DRIVERS);
				if(!CommonUtil.isEmpty(drivers)) {
					String[] ds = drivers.split("\\,");
					for(String d : ds) {
						d = d.trim();
						try {
							Class.forName(d);
							existsDrivers = true;
						} catch (ClassNotFoundException e) {
							log.error("Error when loading db driver: " + d, e);
							throw new RuntimeException("Error when loading db driver: " + d, e);
						}
					}
					
					existsDrivers = true;
				}
			}
			
			if(!existsDrivers) {
				throw new RuntimeException("no suitable driver class be found.");
			}
			
			parseCulsterProp(clusterProp);
		}else{
			log.error("Could not found db config file: " + dbConf);
			throw new RuntimeException("Could not found db config file: " + dbConf);
		}
	}
	
	
	private final static String CLUSTER_NODE_CONF_REGEX = "^cluster\\.(.*)\\.jdbc\\.(.*)$";
	private final static Pattern CLUSTER_NODE_CONF_PATTERN = Pattern.compile(CLUSTER_NODE_CONF_REGEX);
	private void parseCulsterProp(Properties clusterProp) {
		// remove the cluster keys
		clusterProp.remove(CLUSTER_DB_DRIVER);
		clusterProp.remove(CLUSTER_DB_DRIVERS);
		clusterProp.remove(CLUSTER_DB_POOL_SIZE);
		String jdbcUrl = (String)clusterProp.remove(CLUSTER_DB_URL);
		String jdbcUser = (String)clusterProp.remove(CLUSTER_DB_USER);
		String jdbcPassword = (String)clusterProp.remove(CLUSTER_DB_PASSWORD);
		int maxPoolSize = CommonUtil.parseAsInt(clusterProp.remove(CLUSTER_DB_POOL_SIZE), 10);
		
		NodeDBFactory node;
		
		String key;
		String nodeName;
		String confName;
		
		for(Object keyObj : clusterProp.keySet()) {
			key = (String)keyObj;
			
			Matcher matcher = CLUSTER_NODE_CONF_PATTERN.matcher(key);
			if(matcher.matches()) {
				nodeName = matcher.group(1);
				confName = matcher.group(2);
				
				node = clusterNodes.get(nodeName);
				if(node == null) {
					node = new NodeDBFactory();
					node.setConf("url", jdbcUrl);
					node.setConf("user", jdbcUser);
					node.setConf("password", jdbcPassword);
					node.setConf("maxPoolSize", maxPoolSize);
					
					node.nodeName = nodeName;
					
					clusterNodes.put(nodeName, node);
					nodeDBFactorys.add(node);
				}
				
				node.setConf(confName, clusterProp.get(keyObj));
			} else {
				log.warn("ignore the invalid conf for cluster db: " + key);
			}
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
	
	private int getCurrentIdx() {
		Integer idx = nodeRetrieveIdx.get();
		if(idx == null) {
			idx = (int)(loopIdx.getAndIncrement() % nodeDBFactorys.size());
			nodeRetrieveIdx.set(idx);
		}
		
		return idx;
	}
	
	private NodeDBFactory getCurrentFactory(boolean changeable) {
		int curIdx = getCurrentIdx();
		
		NodeDBFactory node;
		
		if(changeable) {
			int nIdx = curIdx;
			do {
				node = nodeDBFactorys.get(nIdx);
				
				if(node.available()) {
					nodeRetrieveIdx.set(nIdx);
					break;
				} else {
					nIdx = (nIdx + 1) % nodeDBFactorys.size();
					if(nIdx == curIdx) { // get tried the full loop
						long waitTs = node.waitTimestamp();
						if(waitTs > 0) {
							try {
								Thread.sleep(waitTs);
							} catch (InterruptedException e) {
								throw new RuntimeException("thread break.", e);
							}
						}
					}
				}
			} while(true);
		} else {
			node = nodeDBFactorys.get(curIdx);
		}
		
		return node;
	}
	
	private void resetCurrentIdx() {
		nodeRetrieveIdx.set(null);
	}
	
	private Connection retrieveConnection() {
		
		Connection connection = null;
		
		NodeDBFactory node = getCurrentFactory(false);
		
		while(node.connPool.size() > 0 || node.usingConnPool.size() >= node.maxPoolSize) {
			connection = node.connPool.poll();
			
			if(connection != null) {
				try {
					if(connection.isValid(0)){
						
						node.usingConnPool.add(connection);
						
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
				Iterator<Connection> conns = node.usingConnPool.iterator();
				Connection conn;
				while(conns.hasNext()) {
					conn = conns.next();
					
					try {
						if(conn.isClosed()){
							node.usingConnPool.remove(conn);
						}
					} catch (SQLException e) {
						
						try{
							conn.close();
						}catch(SQLException e1){
							// skip it
						}
						
						node.usingConnPool.remove(conn);
					}
						
					if(node.usingConnPool.size() < node.maxPoolSize){
						break;
					}
				}
				
				if(node.usingConnPool.size() >= node.maxPoolSize) {
					synchronized (node.connPool) {
						try {
							node.connPool.wait(1000);
						} catch (InterruptedException e) {
							log.error("error while wait for the connection.", e);
						}
					}
				}
			}
		}
		
		synchronized (node.connCreationLock) {
			if(node.usingConnPool.size() < node.maxPoolSize){
				try {
					connection = DriverManager.getConnection(node.prop.getProperty(DB_URL), 
															 node.prop.getProperty(DB_USER), 
															 node.prop.getProperty(DB_PASSWORD));
					
					node.usingConnPool.add(connection);
				} catch (SQLException e) {
					log.error("Error when construct the db connection with node: " + node, e);
					//throw new RuntimeException("Error when construct the db connection.", e);
					node.setToWait();
					resetCurrentIdx();
					return retrieveConnection();
				}
			}
		}
		
		if(connection == null) {
			synchronized (node.connPool) {
				try {
					node.connPool.wait(1000);
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
				
				NodeDBFactory node = getCurrentFactory(true);
				
				synchronized (node.connPool) {
					node.connPool.add((Connection)o);
					node.usingConnPool.remove(o);
					
					node.connPool.notify();
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
}