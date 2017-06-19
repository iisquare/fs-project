package com.iisquare.etl.spark.flow;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.spark.api.java.JavaSparkContext;

public abstract class Node {

	protected JavaSparkContext sparkContext;
	protected boolean isReady = false;
	protected Set<Node> source = new HashSet<>();
	protected Set<Node> target = new HashSet<>();
	protected Properties properties;
	protected Object result;
	
	public JavaSparkContext getSparkContext() {
		return sparkContext;
	}

	public void setSparkContext(JavaSparkContext sparkContext) {
		this.sparkContext = sparkContext;
	}

	public boolean isReady() {
		return isReady;
	}
	
	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}
	
	public Set<Node> getSource() {
		return source;
	}
	
	public void setSource(Set<Node> source) {
		this.source = source;
	}
	
	public Set<Node> getTarget() {
		return target;
	}
	
	public void setTarget(Set<Node> target) {
		this.target = target;
	}
	
	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public abstract Object process();
	
}
