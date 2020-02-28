package com.iisquare.jwframe.database;

/**
 * JDBC连接器基类
 * @author Ouyang <iisquare@163.com>
 *
 */
public abstract class Connector {
	
	public static final String DBTYPE_MYSQL = "mysql";
	public static final String JDBCDRIVER_MYSQL = "com.mysql.jdbc.Driver";
	
	protected String jdbcDriver;
	protected String username;
	protected String password;
	protected Boolean isCheckValid;
	protected Integer incrementalConnections;
	protected Integer decrementalConnections;
	protected Integer initialConnections;
	protected Integer maxConnections;
	protected Integer timeEventInterval;
	
	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean getIsCheckValid() {
		return isCheckValid;
	}

	public void setIsCheckValid(Boolean isCheckValid) {
		this.isCheckValid = isCheckValid;
	}

	public Integer getIncrementalConnections() {
		return incrementalConnections;
	}

	public void setIncrementalConnections(Integer incrementalConnections) {
		this.incrementalConnections = incrementalConnections;
	}

	public Integer getDecrementalConnections() {
		return decrementalConnections;
	}

	public void setDecrementalConnections(Integer decrementalConnections) {
		this.decrementalConnections = decrementalConnections;
	}

	public Integer getInitialConnections() {
		return initialConnections;
	}

	public void setInitialConnections(Integer initialConnections) {
		this.initialConnections = initialConnections;
	}

	public Integer getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(Integer maxConnections) {
		this.maxConnections = maxConnections;
	}

	public Integer getTimeEventInterval() {
		return timeEventInterval;
	}

	public void setTimeEventInterval(Integer timeEventInterval) {
		this.timeEventInterval = timeEventInterval;
	}

}
