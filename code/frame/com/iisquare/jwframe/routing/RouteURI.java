package com.iisquare.jwframe.routing;

public class RouteURI {

	private String[] methods;
	private String uri;
	private Generator action;
	
	public String[] getMethods() {
		return methods;
	}
	
	public RouteURI setMethods(String[] methods) {
		this.methods = methods;
		return this;
	}
	
	public String getUri() {
		return uri;
	}
	
	public RouteURI setUri(String uri) {
		this.uri = "^" + uri.replaceAll("\\.", "\\\\.").replaceAll("\\{\\w+\\}", "(.*?)") + "$";
		return this;
	}
	
	public Generator getAction() {
		return action;
	}
	
	public RouteURI setAction(Generator action) {
		this.action = action;
		return this;
	}

	public RouteURI(String[] methods, String uri, Generator action) {
		this.methods = methods;
		setUri(uri);
		this.action = action;
	}
	
}
