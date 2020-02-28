package com.iisquare.jwframe.database;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.PropertiesUtil;

/**
 * JDBC连接器管理基类
 * @author Ouyang <iisquare@163.com>
 *
 */
public abstract class ConnectorManager {

	protected ConnectionManager connectionManager;
    private static Map<String, Map<String, Object>> config;
    
    public ConnectorManager(ConnectionManager connectionManager) {
    	this.connectionManager = connectionManager;
    }
    
    protected static Map<String, Object> loadConfig(String dbType) {
        if(null == config) {
        	synchronized (ConnectorManager.class) {
        		if(null == config) {
        			config = new Hashtable<>();
        			Properties prop = PropertiesUtil.load(ConnectorManager.class.getClassLoader(), "jdbc.properties");
        			if(!DPUtil.empty(prop.getProperty(Connector.DBTYPE_MYSQL))) {
        				config.put(Connector.DBTYPE_MYSQL, loadMySQLConfig(prop));
        			}
        		}
        	}
        }
        if(null == dbType) return null;
        return config.get(dbType);
    }
    
    private static Map<String, Object> loadMySQLConfig(Properties prop) {
    	Map<String, Object> configMap = new HashMap<>();
    	configMap.put("dbname", prop.getProperty("mysql.dbname"));
    	configMap.put("username", prop.getProperty("mysql.username"));
    	configMap.put("password", prop.getProperty("mysql.password"));
    	configMap.put("charset", prop.getProperty("mysql.charset"));
    	configMap.put("tablePrefix", prop.getProperty("mysql.tablePrefix"));
    	// 连接池配置
		configMap.put("isCheckValid", prop.getProperty("mysql.isCheckValid"));
		configMap.put("incrementalConnections", prop.getProperty("mysql.incrementalConnections"));
		configMap.put("decrementalConnections", prop.getProperty("mysql.decrementalConnections"));
		configMap.put("initialConnections", prop.getProperty("mysql.initialConnections"));
		configMap.put("maxConnections", prop.getProperty("mysql.maxConnections"));
		configMap.put("timeEventInterval", prop.getProperty("mysql.timeEventInterval"));
    	String[] stringArray;
    	// 主库配置
    	String master = prop.getProperty("mysql.master");
    	Map<String, Object> masterMap = new HashMap<>();
    	stringArray = DPUtil.explode(master, ":", " ", false);
    	if(2 == stringArray.length) {
    		masterMap.put("host", stringArray[0]);
    		masterMap.put("port", stringArray[1]);
    	}
    	configMap.put("master", masterMap);
    	// 从库配置
    	String slaves = prop.getProperty("mysql.slaves");
    	List<Map<String, Object>> slavesList = new Vector<>();
    	for (String slave : DPUtil.explode(slaves, ",", " ", false)) {
    		stringArray = DPUtil.explode(slave, ":", " ", false);
    		if(2 != stringArray.length) continue;
    		Map<String, Object> slaveMap = new HashMap<>();
    		slaveMap.put("host", stringArray[0]);
    		slaveMap.put("port", stringArray[1]);
    		slavesList.add(slaveMap);
    	}
    	configMap.put("slaves", slavesList);
    	return configMap;
    }
    
}
