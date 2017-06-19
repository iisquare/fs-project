package com.iisquare.etl.spark.flow;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public abstract class Node {

	protected boolean isReady = false;
	protected Set<Node> source = new HashSet<>();
	protected Set<Node> target = new HashSet<>();
	protected Properties properties;
	
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

	public abstract Object process();
	
}
