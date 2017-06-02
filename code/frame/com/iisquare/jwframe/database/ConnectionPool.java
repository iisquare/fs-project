package com.iisquare.jwframe.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * JDBC连接池
 * @author Ouyang <iisquare@163.com>
 *
 */
public class ConnectionPool {
	private String jdbcDriver = ""; // 数据库驱动
	private String dbUrl = ""; // 数据 URL
	private String dbUsername = ""; // 数据库用户名
	private String dbPassword = ""; // 数据库用户密码
	private boolean isCheckValid = true; // 检查连接是否可用
	private int initialConnections = 1; // 连接池的初始大小
	private int incrementalConnections = 5;// 连接池自动增加的大小，0固定连接池大小，-1由程序判断
	private int decrementalConnections = 5;// 连接池自动减少的大小，0不释放空闲连接，-1由程序判断
	private int maxConnections = 20; // 连接池最大的大小
	private long timeEventInterval = 4 * 60 * 60 * 1000; // 4h
	private Vector<DBPooledConnection> connections = null; // 存放连接池中数据库连接的向量
	private Driver driver = null; // 数据库驱动
	private int usedConnectionSize = 0;
	private long lastTimerEvent = System.currentTimeMillis();
	private Logger logger = Logger.getLogger(getClass().getName());

	public ConnectionPool(String jdbcDriver, String dbUrl, String dbUsername, String dbPassword) {

		this.jdbcDriver = jdbcDriver;
		this.dbUrl = dbUrl;
		this.dbUsername = dbUsername;
		this.dbPassword = dbPassword;
		try {
			createPool();
		} catch (Exception e) {
			if(logger.isDebugEnabled()) logger.debug("Create connection pool is fialed!");
		}
	}

	public int getInitialConnections() {
		return this.initialConnections;
	}

	public void setInitialConnections(int initialConnections) {
		this.initialConnections = initialConnections;
	}

	public int getIncrementalConnections() {
		return this.incrementalConnections;
	}

	public void setIncrementalConnections(int incrementalConnections) {
		this.incrementalConnections = incrementalConnections;
	}

	public int getDecrementalConnections() {
		return decrementalConnections;
	}

	public void setDecrementalConnections(int decrementalConnections) {
		this.decrementalConnections = decrementalConnections;
	}

	public int getMaxConnections() {
		return this.maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public boolean isCheckValid() {
		return isCheckValid;
	}

	public void setCheckValid(boolean isCheckValid) {
		this.isCheckValid = isCheckValid;
	}

	public long getTimeEventInterval() {
		return timeEventInterval;
	}

	public void setTimeEventInterval(long timeEventInterval) {
		this.timeEventInterval = timeEventInterval;
	}

	public int getUsedConnectionSize() {
		return usedConnectionSize;
	}

	public synchronized void createPool() throws Exception {
		if (connections != null) {
			return;
		}
		driver = (Driver) (Class.forName(this.jdbcDriver).newInstance());
		DriverManager.registerDriver(driver);
		connections = new Vector<DBPooledConnection>();
		createConnections(this.initialConnections);
		if(logger.isDebugEnabled()) logger.debug("Connection pool is created!");
	}

	/**
	 * 创建由 numConnections 指定数目的数据库连接 , 并把这些连接放入 connections 向量中
	 */
	private synchronized void createConnections(int numConnections) throws SQLException {
		for (int x = 0; x < numConnections; x++) {
			if (this.maxConnections > 0 && this.connections.size() >= this.maxConnections) {
				if(logger.isDebugEnabled()) logger.debug("Connection pool is full!");
				break;
			}
			try {
				connections.addElement(new DBPooledConnection(newConnection()));
			} catch (SQLException e) {
				if(logger.isDebugEnabled()) logger.debug("Create new connection is failed!" + e.getMessage());
				throw new SQLException();
			}
		}
	}

	private synchronized Connection newConnection() throws SQLException {
		Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);

		// 如果这是第一次创建数据库连接，即检查数据库，获得此数据库允许支持的最大客户连接数目
		// connections.size()==0 表示目前没有连接己被创建
		if (connections.size() == 0) {
			DatabaseMetaData metaData = conn.getMetaData();
			int driverMaxConnections = metaData.getMaxConnections();
			// 数据库返回的 driverMaxConnections 若为 0 ，表示此数据库没有最大连接限制，或数据库的最大连接限制不知道
			// driverMaxConnections 为返回的一个整数，表示此数据库允许客户连接的数目
			if (driverMaxConnections > 0 && this.maxConnections > driverMaxConnections) {
				this.maxConnections = driverMaxConnections;
			}
		}
		return conn;
	}
	
	public synchronized Connection getConnection() throws SQLException {
		timerEvent();
		if (connections == null) {
			return null;
		}
		DBPooledConnection conn = getFreeConnection();
//		while (conn == null) {
//			wait(250);
//			conn = getFreeConnection();
//		}这种方式会导致returnConnection无法执行
		if(conn != null) {
			usedConnectionSize++;
			return conn.getConnection();
		}
		return null;
	}

	private DBPooledConnection getFreeConnection() throws SQLException {
		DBPooledConnection conn = findFreeConnection();
		if (conn == null) {
			createConnections(getIncreasingConnectionCount());
			conn = findFreeConnection();
			if (conn == null) {
				return null;
			}
		}
		return conn;
	}

	private DBPooledConnection findFreeConnection() throws SQLException {
		for (int i = 0; i < connections.size(); i++) {
			DBPooledConnection pc = connections.elementAt(i);
			if (!pc.isBusy()) {
				Connection conn = pc.getConnection();
				pc.setBusy(true);
				if (isCheckValid && !conn.isValid(3000)) { // 超时时间设置，性能影响点
					// 如果此连接不可再用了，则创建一个新的连接，
					// 并替换此不可用的连接对象，如果创建失败，删除该无效连接，遍历下一个不忙连接
					try {
						conn = newConnection();
						pc.setConnection(conn);
					} catch (SQLException e) {
						e.printStackTrace();
						connections.remove(i--);
						continue;
					}
				}
				return pc;
			}
		}
		return null;

	}

	public synchronized void returnConnection(Connection conn) {
		if (connections == null) {
			if(logger.isDebugEnabled()) logger.debug("Connection pool is not exist!");
			return;
		}
		DBPooledConnection pConn = null;
		Enumeration<DBPooledConnection> enumerate = connections.elements();
		while (enumerate.hasMoreElements()) {
			pConn = (DBPooledConnection) enumerate.nextElement();
			if (conn == pConn.getConnection()) {
				pConn.setBusy(false);
				usedConnectionSize--;
				break;
			}
		}
	}

	public synchronized void refreshConnections() throws SQLException {
		if (connections == null) {
			return;
		}
		int decreaseSize = getDecreasingConnectionCount();
		int totalSize = connections.size();
		for (int i = totalSize - 1; i >= 0 && decreaseSize > 0; i--) {
			DBPooledConnection pc = connections.elementAt(i);
			if (!pc.isBusy()) {
				pc.setBusy(true);
				closeConnection(pc.getConnection());
				connections.remove(i);
				decreaseSize--;
			}
		}
	}

	public synchronized void closeConnectionPool() throws SQLException {
		if (connections == null) {
			return;
		}
		DBPooledConnection pConn = null;
		Enumeration<DBPooledConnection> enumerate = connections.elements();
		while (enumerate.hasMoreElements()) {
			pConn = (DBPooledConnection) enumerate.nextElement();
			if (pConn.isBusy()) {
				wait(5000);
			}
			closeConnection(pConn.getConnection());
			connections.removeElement(pConn);
		}
		connections = null;
		usedConnectionSize = 0;
		DriverManager.deregisterDriver(driver);
	}

	private void closeConnection(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			if(logger.isDebugEnabled()) logger.debug("Close connection has something wrong!" + e.getMessage());
		}

	}

	private void wait(int mSeconds) {
		try {
			Thread.sleep(mSeconds);
		} catch (InterruptedException e) {
		}
	}
	
	private void timerEvent() throws SQLException {
		long time = System.currentTimeMillis();
		if (time < lastTimerEvent) {
			time = lastTimerEvent;
			return;
		}
		if (time - lastTimerEvent < timeEventInterval) {
			return;
		}
		lastTimerEvent = time;
		refreshConnections();
	}

	public synchronized int getIncreasingConnectionCount() {
		if(incrementalConnections >= 0) return incrementalConnections;
		int count = 1;
		count = connections.size() / 4;
		if (count < 1) {
			count = 1;
		}
		return count;
	}

	public synchronized int getDecreasingConnectionCount() {
		if(decrementalConnections >= 0) return decrementalConnections;
		int totalSize = connections.size();
		int count = (totalSize - usedConnectionSize) / 3;
		if(totalSize - count <= initialConnections)count = 0;
		if(count == 0 && count + usedConnectionSize > initialConnections)count = 1;
		return count;
	}

	public synchronized void printDebugMsg(String prefixMessage, String postfixMessage) {
		if (connections == null) {
			return;
		}
		if(logger.isDebugEnabled()) logger.debug(
				prefixMessage + "Connection pool: totalSize - "
				+ connections.size() + " usedSize - " + usedConnectionSize + postfixMessage);
	}
	
	public synchronized int getNotUsedConnectionCount() {
		return connections.size() - usedConnectionSize;
	}

	public synchronized int getUsedConnectionCount() {
		return usedConnectionSize;
	}

	public synchronized int getConnectionCount() {
		return connections.size();
	}

	class DBPooledConnection {
		private Connection connection = null;// 数据库连接
		private boolean busy; // 此连接是否正在使用的标志，默认没有正在使用

		private DBPooledConnection(Connection connection) {
			this.connection = connection;
		}

		private Connection getConnection() {
			return connection;
		}

		private void setConnection(Connection connection) {
			this.connection = connection;
		}

		private boolean isBusy() {
			return busy;
		}
		
		private void setBusy(boolean busy) {
			this.busy = busy;
		}

		public void close() {
			busy = false;
		}
	}
}
