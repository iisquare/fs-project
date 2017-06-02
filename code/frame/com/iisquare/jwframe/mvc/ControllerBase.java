package com.iisquare.jwframe.mvc;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.Configuration;
import com.iisquare.jwframe.freemarker.FreeMarkerConfigurer;

import freemarker.template.Template;

@Controller
@Scope("prototype")
public abstract class ControllerBase {
	
	protected WebApplicationContext webApplicationContext;
	protected String appUri, appUrl, appPath, rootPath;
	protected String moduleName, controllerName, actionName;

	protected HttpServletRequest request;
	protected HttpServletResponse response;

	protected Map<String, String[]> params; // 请求参数
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

	public Map<String, String[]> getParams() {
		return params;
	}

	public void setParams(Map<String, String[]> params) {
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

	protected String getParam(String key) {
        return getParam(key, null);
    }
	
	protected String getParam(String key, String defaultValue) {
        if(null == key) return null;
        if(!params.containsKey(key)) return defaultValue;
        String[] values = params.get(key);
        if(null == values || 0 == values.length) return null;
        return values[0];
    }
	
	/**
	 * 设置视图中需要的参数
	 */
	protected void assign(String key, Object value) {
		assign.put(key, value);
	}

	protected String url() {
		return url(controllerName, actionName);
	}

	protected String url(String action) {
		return url(controllerName, action);
	}

	/**
	 * 获取URL地址
	 */
	protected String url(String controller, String action) {
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
		Template template = webApplicationContext.getBean(FreeMarkerConfigurer.class).getConfiguration()
				.getTemplate(moduleName + "/" + fileUri + webApplicationContext.getBean(Configuration.class).getTemplateSuffix());
		Writer out = new OutputStreamWriter(response.getOutputStream());
		template.process(dataModel, out);
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
		String result;
		if (object instanceof Map) {
			result = JSONObject.fromObject(object).toString();
		} else {
			result = JSONArray.fromObject(object).toString();
		}
		return displayText(result);
	}

	/**
	 * 重定向自定义URL地址
	 */
	protected Object redirect(String url) throws Exception {
		response.sendRedirect(url);
		return null;
	}
}
