package com.iisquare.jwframe.mvc;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.Configuration;
import com.iisquare.jwframe.freemarker.FreeMarkerConfigurer;
import com.iisquare.jwframe.utils.DPUtil;

import freemarker.template.Template;

@Controller
@Scope("prototype")
public abstract class ControllerBase {

	protected WebApplicationContext webApplicationContext;
	protected String appUri, appUrl, appPath, rootPath;
	protected String moduleName, controllerName, actionName;

	protected HttpServletRequest request;
	protected HttpServletResponse response;

	protected Map<String, Object> params; // 请求参数
	protected Map<String, Object> assign; // 视图数据Map对象

	public WebApplicationContext getWebApplicationContext() {
		return webApplicationContext;
	}

	public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
		this.webApplicationContext = webApplicationContext;
	}

	public String getAppUri() {
		return appUri;
	}

	public void setAppUri(String appUri) {
		this.appUri = appUri;
	}

	public String getAppUrl() {
		return appUrl;
	}

	public void setAppUrl(String appUrl) {
		this.appUrl = appUrl;
	}

	public String getAppPath() {
		return appPath;
	}

	public void setAppPath(String appPath) {
		this.appPath = appPath;
	}

	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getControllerName() {
		return controllerName;
	}

	public void setControllerName(String controllerName) {
		this.controllerName = controllerName;
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public HttpServletResponse getResponse() {
		return response;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}

	public Map<String, Object> getAssign() {
		return assign;
	}

	public void setAssign(Map<String, Object> assign) {
		this.assign = assign;
	}

	public Object init() {
		return null;
	}

	public Object destroy(Object actionVal) {
		return actionVal;
	}

	protected boolean hasParam(String key) {
		return params.containsKey(key);
	}
	
	protected String getParam(String key) {
		return getParam(key, null);
	}

	protected String getParam(String key, String defaultValue) {
		if (null == key) return null;
		if (!params.containsKey(key)) return defaultValue;
		Object value = params.get(key);
		if (null == value) return null;
		return value.toString();
	}
	
	/**
	 * 获取请求参数数组
	 */
	protected String[] getArray(String key) {
		Object value = params.get(key);
		if(null == value || !value.getClass().isArray()) return new String[]{};
		return (String[]) value;
	}
	
	/**
	 * 获取请求参数Map
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> getMap(String key) {
		Object value = params.get(key);
		if(null == value || !(value instanceof Map)) return new LinkedHashMap<String, Object>();
		return (Map<String, Object>) value;
	}

	/**
	 * 设置视图中需要的参数
	 */
	protected void assign(String key, Object value) {
		assign.put(key, value);
	}

	public String url() {
		return url(controllerName, actionName);
	}

	public String url(String action) {
		return url(controllerName, action);
	}

	/**
	 * 获取URL地址
	 */
	public String url(String controller, String action) {
		return appPath + controller + "/" + action + "/";
	}

	protected Object displayTemplate() throws Exception {
		return displayTemplate(controllerName, actionName);
	}

	protected Object displayTemplate(String action) throws Exception {
		return displayTemplate(controllerName, action);
	}

	protected Object displayTemplate(String controller, String action) throws Exception {
		return displayTemplate(controller + "/" + action, assign);
	}

	protected Object displayTemplate(String fileUri, Object dataModel) throws Exception {
		Template template = webApplicationContext.getBean(
			FreeMarkerConfigurer.class).getConfiguration().getTemplate(moduleName
			+ "/" + fileUri + webApplicationContext.getBean(Configuration.class).getTemplateSuffix());
		template.process(dataModel, response.getWriter());
		return null;
	}

	/**
	 * 输出文本信息
	 */
	protected Object displayText(String text) throws Exception {
		PrintWriter out = response.getWriter();
		out.print(text);
		out.flush();
		return null;
	}

	/**
	 * 将assign中的数据输出为JSON格式
	 */
	protected Object displayJSON() throws Exception {
		return displayJSON(assign);
	}

	/**
	 * 输出JSON信息
	 */
	protected Object displayJSON(Object object) throws Exception {
		return displayText(DPUtil.stringifyJSON(object));
	}

	/**
	 * 重定向自定义URL地址
	 */
	protected Object redirect(String url) throws Exception {
		response.sendRedirect(url);
		return null;
	}
}
