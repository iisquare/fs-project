package com.iisquare.etl.spark.config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.spark.SparkConf;

import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.PropertiesUtil;

public class Configuration implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String CONFIG_FILE_NAME = "spark.properties";
	private static Configuration instance;
	private Properties properties;
	
	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public static Configuration getInstance() {
		if(null == instance) {
			synchronized (Configuration.class) {
				if(null == instance) {
					instance = new Configuration();
					instance.init();
				}
			}
		}
		return instance;
	}
	
	private void init() {
		properties = PropertiesUtil.load(getClass().getClassLoader(), CONFIG_FILE_NAME);
	}
	
	public Properties reduce(String... keys) {
		Properties prop = new Properties();
		for (String key : keys) {
			prop.setProperty(key, properties.getProperty(key));
		}
		return prop;
	}
	
	public Object setProperty(String key, String value) {
		return properties.setProperty(key, value);
	}
	
	public String getProperty(String key) {
		return properties.getProperty(key);
	}
	
	public String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}
	
	public SparkConf getSparkConf() {
		return new SparkConf().setMaster(getProperty("master")).setAppName(getProperty("app.name"));
	}
	
	public SparkConf fillSparkConfWithElasticseearch(SparkConf sparkConf) {
		if(null == sparkConf) return sparkConf;
		sparkConf.set("es.nodes", getProperty("es.nodes", "127.0.0.1"));
		sparkConf.set("es.port", getProperty("es.port", "9200"));
		sparkConf.set("es.index.auto.create", getProperty("es.index.auto.create", "false"));
		String user = getProperty("es.net.http.auth.user");
		if(!DPUtil.empty(user)) {
			sparkConf.set("es.net.http.auth.user", user);
			sparkConf.set("es.net.http.auth.pass", getProperty("es.net.http.auth.pass"));
		}
		return sparkConf;
	}
	
	public org.apache.hadoop.conf.Configuration getHBaseConfiguration() {
		org.apache.hadoop.conf.Configuration configHBase = HBaseConfiguration.create();
		configHBase.set("hbase.zookeeper.property.clientPort", getProperty("hbase.zookeeper.property.clientPort", "2181"));
		configHBase.set("hbase.zookeeper.quorum", getProperty("hbase.zookeeper.quorum"));
		return configHBase;
	}
	
	public Map<String, String> getKafkaParams() {
		Map<String, String> kafkaParams = new HashMap<String, String>();
		kafkaParams.put("metadata.broker.list", getProperty("metadata.broker.list"));
		kafkaParams.put("group.id", getProperty("group.id"));
		return kafkaParams;
	}
	
	public Map<String, String> getMySQLOptions(String table, LinkedHashMap<String, String> replace) {
		// 默认参数
		String host = getProperty("mysql.host.default", "127.0.0.1");
		String port = getProperty("mysql.port.default", "3306");
		String user = getProperty("mysql.user.default", "root");
		String password = getProperty("mysql.password.default", "");
		String charset = getProperty("mysql.charset.default", "UTF-8");
		String db = getProperty("mysql.db.default");
		// 表参数
		String dbName = getProperty("mysql.db." + table, db);
		String tableName = getProperty("mysql.table." + table);
		Map<String, String> mysqlOptions = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder("jdbc:mysql://");
		sb.append(getProperty("mysql.host." + table, host));
		sb.append(":").append(getProperty("mysql.port." + table, port));
		if(null != dbName) sb.append("/").append(dbName);
		if(null != charset) sb.append("?characterEncoding=").append(getProperty("mysql.charset." + table, charset));
		mysqlOptions.put("url", sb.toString());
		mysqlOptions.put("driver", getProperty("mysql.driver", "com.mysql.cj.jdbc.Driver"));
		mysqlOptions.put("user", getProperty("mysql.user." + table, user));
		mysqlOptions.put("password", getProperty("mysql.password." + table, password));
		mysqlOptions.put("dbtable", tableName);
		if(null == replace) return mysqlOptions;
		for (Entry<String, String> replaceEntry : replace.entrySet()) {
			String replaceKey = replaceEntry.getKey();
			String replaceValue = replaceEntry.getValue();
			for (Entry<String, String> option : mysqlOptions.entrySet()) {
				String value = option.getValue();
				if(-1 == value.indexOf("${" + replaceKey + "}")) continue;
				mysqlOptions.put(option.getKey(), value.replaceAll("\\$\\{" + replaceKey + "\\}", replaceValue));
			}
		}
		return mysqlOptions;
	}
	
	public Set<String> getKafkaTopics() {
		return new HashSet<String>(Arrays.asList(getProperty("topics").split(",")));
	}
	
}
