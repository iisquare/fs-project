package com.iisquare.jwframe.routing;

import java.util.Map;

public class RouteAction {
	
	private String controllerName;
	private String actionName;
	private Map<String, String[]> params;
	
	public String getControllerName() {
		return controllerName;
	}

	public RouteAction setControllerName(String controllerName) {
		this.controllerName = controllerName;
		return this;
	}

	public String getActionName() {
		return actionName;
	}

	public RouteAction setActionName(String actionName) {
		this.actionName = actionName;
		return this;
	}

	public Map<String, String[]> getParams() {
		return params;
	}

	public RouteAction setParams(Map<String, String[]> params) {
		this.params = params;
		return this;
	}

	public RouteAction(String controllerName, String actionName, Map<String, String[]> params) {
		this.controllerName = controllerName;
		this.actionName = actionName;
		this.params = params;
	}
	
}
