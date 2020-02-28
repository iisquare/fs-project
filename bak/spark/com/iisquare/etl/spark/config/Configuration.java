package com.iisquare.etl.spark.config;

import java.io.Serializable;
import java.util.Properties;
import org.apache.spark.SparkConf;

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
	
}
