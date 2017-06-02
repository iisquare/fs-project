package com.iisquare.jwframe.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import com.iisquare.jwframe.utils.DPUtil;

/**
 * MySQL主从连接管理类
 * 若多个DAO使用同一个实例，仅在最后一次执行commit/rollback的时候，事务才生效。
 * 开发者需要自行判断多个DAO是否使用同一个实例，用来处理事务的最终状态。
 * @author Ouyang <iisquare@163.com>
 *
 */
public class MySQLConnector extends Connector {

	private String dbName;
	private String charset;
	private String tablePrefix;
	private String masterHost;
    private int masterPort;
    private String slaveHost;
    private int slavePort;
    private Connection masterResource;
    private Connection slaveResource;
    private int timeout = 250; // 获取连接超时时间
    private int transactionLevel = 0;
    private String masterUrl, slaveUrl;
    private ConnectionManager connectionManager;
    private Logger logger = Logger.getLogger(getClass().getName());
    
    public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
     * 获取数据库表前缀
     */
	public String getTablePrefix() {
		return tablePrefix;
	}

	@SuppressWarnings("unchecked")
	public MySQLConnector(ConnectionManager connectionManager,
			Map<String, Object> config, String dbName, String charset) {
		this.connectionManager = connectionManager;
		jdbcDriver = JDBCDRIVER_MYSQL;
		this.dbName = dbName;
		this.charset = charset;
		Object temp;
		temp = config.get("username");
		if(null != temp) username = DPUtil.parseString(temp);
		temp = config.get("password");
		if(null != temp) password = DPUtil.parseString(temp);
		temp = config.get("tablePrefix");
		if(null != temp) tablePrefix = DPUtil.parseString(temp);
		temp = config.get("isCheckValid");
		if(null != temp) isCheckValid = !DPUtil.empty(temp);
		temp = config.get("incrementalConnections");
		if(null != temp) incrementalConnections = DPUtil.parseInt(temp);
		temp = config.get("decrementalConnections");
		if(null != temp) decrementalConnections = DPUtil.parseInt(temp);
		temp = config.get("initialConnections");
		if(null != temp) initialConnections = DPUtil.parseInt(temp);
		temp = config.get("maxConnections");
		if(null != temp) maxConnections = DPUtil.parseInt(temp);
		temp = config.get("timeEventInterval");
		if(null != temp) timeEventInterval = DPUtil.parseInt(temp);
		Map<String, Object> masterMap = (Map<String, Object>) config.get("master");
		masterHost = DPUtil.parseString(masterMap.get("host"));
		masterPort = DPUtil.parseInt(masterMap.get("port"));
		List<Map<String, Object>> slavesList = (List<Map<String, Object>>) config.get("slaves");
		if(slavesList.isEmpty()) {
			slaveHost = masterHost;
			slavePort = masterPort;
		} else {
			Map<String, Object> slaverMap = slavesList.get(new Random().nextInt(slavesList.size()));
			slaveHost =  DPUtil.parseString(slaverMap.get("host"));;
			slavePort = DPUtil.parseInt(slaverMap.get("port"));
		}
		masterUrl = "jdbc:mysql://" + masterHost + ":" + masterPort + "/" + this.dbName;
		slaveUrl = "jdbc:mysql://" + slaveHost + ":" + slavePort + "/" + this.dbName;
		if(!DPUtil.empty(this.charset)) {
			masterUrl += "?characterEncoding=" + this.charset;
			slaveUrl += "?characterEncoding=" + this.charset;
		}
	}
	
    private Connection getResource(boolean isMaster) {
    	String dbUrl = isMaster ? masterUrl : slaveUrl;
        if(!connectionManager.addPool(this, dbUrl)) return null;
        try {
			return connectionManager.getConnection(dbUrl, timeout);
		} catch (SQLException e) {
			logger.error(e.getMessage());
			return null;
		}
    }
    
    public void close() {
    	if(null != masterResource) {
    		connectionManager.returnConnection(masterUrl, masterResource);
    		masterResource = null;
    	}
    	if(null != slaveResource) {
    		connectionManager.returnConnection(slaveUrl, slaveResource);
    		slaveResource = null;
    	}
    }

    public Connection getMaster() {
        if(logger.isDebugEnabled()) {
            logger.debug("MySQLConnection - [master]" + (null == masterResource ? "create" : "hit")
                + ":{host:" + masterHost + ";port:" + masterPort + ";dbname:" + dbName + ";dbuser:" + username + "}");
        }
        if(null == masterResource) masterResource = getResource(true);
        return masterResource;
    }
    
    public Connection getSlave() {
        if(logger.isDebugEnabled()) {
            logger.debug("MySQLConnection - [slave]" + (null == slaveResource ? "create" : "hit")
                + ":{host:" + slaveHost + ";port:" + slavePort + ";dbname:" + dbName + ";dbuser:" + username + "}");
        }
        if(null == slaveResource) slaveResource = getResource(false);
        return slaveResource;
    }
    
    public boolean isTransaction() {
        try {
			return transactionLevel > 0 && null != masterResource && !masterResource.getAutoCommit();
		} catch (SQLException e) {
			logger.error(e.getMessage());
			return false;
		}
    }
    
    public boolean beginTransaction() {
        if (logger.isDebugEnabled()) {
            logger.debug("MySQL - Begin transaction by Level:" + transactionLevel);
        }
        if (transactionLevel == 0) {
            transactionLevel = 1;
            try {
				getMaster().setAutoCommit(false);
			} catch (SQLException e) {
				logger.error(e.getMessage());
				return false;
			}
        }
        transactionLevel++;
        return true;
    }
    
    public boolean commit() {
        if (!isTransaction()) {
            return false;
        }
        transactionLevel--;
        if (logger.isDebugEnabled()) {
            logger.debug("Commit transaction by Level:" + transactionLevel);
        }
        if (transactionLevel == 0) {
            try {
				getMaster().commit();
				getMaster().setAutoCommit(true);
				return true;
			} catch (SQLException e) {
				logger.error(e.getMessage());
				return false;
			}
        }
        return true;
    }
    
    public boolean rollback() {
        if (!isTransaction()) {
            return false;
        }
        transactionLevel--;
        if (transactionLevel == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Rollback transaction by Level:" + transactionLevel);
            }
            try {
				getMaster().rollback();
				return true;
			} catch (SQLException e) {
				logger.error(e.getMessage());
				return false;
			}
        }
        return true;
    }

}
