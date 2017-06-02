package com.iisquare.jwframe.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * JDBC连接池管理类
 * 全局(单应用)内唯一，内部维护clients计数器，用来管理连接池。
 * 若频繁getInstance获取实例，会严重影响性能！应用销毁时需要调用release方法，减少引用计数。
 * @author Ouyang <iisquare@163.com>
 *
 */
public class ConnectionManager {
	
	private static ConnectionManager instance; // 唯一实例
	
	private static int clients = 0;
	private static Hashtable<String, ConnectionPool> pools = new Hashtable<>(); // 连接
	private static Logger logger = Logger.getLogger(ConnectionManager.class);

	public static int getClients() {
		return clients;
	}

	private ConnectionManager() {}
	
	public static ConnectionManager getInstance() {
		if (null == instance) {
			synchronized(ConnectionManager.class) {
				if(null == instance) instance = new ConnectionManager();
			}
		}
		clients++;
		if(logger.isDebugEnabled()) logger.debug("ConnectionManager.getInstance - current client count:" + clients);
		return instance;
	}
	
	public void returnConnection(String name, Connection con) {
		if(null == con) return ;
		ConnectionPool pool = pools.get(name);
		if (pool != null) {
			pool.returnConnection(con);
		} else {
			if(logger.isDebugEnabled()) logger.debug("Connection pool witch named " + name + " is null!");
		}
	}

	public Connection getConnection(String name) throws SQLException {
		return getConnection(name, -1);
	}

	/**
	 *  获取JDBC连接
	 * @param name 连接池名称
	 * @param timeout never timeout when the value equals -1
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection(String name, long timeout) throws SQLException {
		ConnectionPool pool = pools.get(name);
		if (pool != null) {
			long startTime = System.currentTimeMillis();
			Connection con;
			while ((con = pool.getConnection()) == null) {
				if (timeout >= 0 && (System.currentTimeMillis() - startTime) >= timeout) {
					return null;
				}
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {}
			}
			return con;
		}
		return null;
	}
	
	public synchronized void release() throws SQLException {
		clients--;
		if(logger.isDebugEnabled()) logger.debug("ConnectionManager.release - current client count:" + clients);
		if (0 != clients) return;
		Enumeration<ConnectionPool> allPools = pools.elements();
		while (allPools.hasMoreElements()) {
			ConnectionPool pool = (ConnectionPool) allPools.nextElement();
			pool.closeConnectionPool();
		}
	}
	
	public void printDebugMsg(String name) {
		ConnectionPool pool = pools.get(name);
		if (pool != null) {
			pool.printDebugMsg("DBConnectionPool[" + name + "] ", "");
		}
	}

	public boolean addPool(Connector connector, String dbUrl) {
		if(pools.contains(dbUrl)) return true;
		synchronized (ConnectionManager.class) {
			if(pools.contains(dbUrl)) return true;
			ConnectionPool pool = new ConnectionPool(
					connector.getJdbcDriver(), dbUrl, connector.getUsername(), connector.getPassword());
			Boolean isCheckValid = connector.getIsCheckValid();
			Integer incrementalConnections = connector.getIncrementalConnections();
			Integer decrementalConnections = connector.getDecrementalConnections();
			Integer initialConnections = connector.getInitialConnections();
			Integer maxConnections = connector.getMaxConnections();
			Integer timeEventInterval = connector.getTimeEventInterval();
			if(null != isCheckValid) pool.setCheckValid(isCheckValid);
			if(null != incrementalConnections) pool.setIncrementalConnections(incrementalConnections);
			if(null != decrementalConnections) pool.setDecrementalConnections(decrementalConnections);
			if(null != initialConnections) pool.setInitialConnections(initialConnections);
			if(null != maxConnections) pool.setMaxConnections(maxConnections);
			if(null != timeEventInterval) pool.setTimeEventInterval(timeEventInterval);
			pools.put(dbUrl, pool);
		}
		return true;
	}
}
