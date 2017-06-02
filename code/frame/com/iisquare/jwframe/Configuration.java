package com.iisquare.jwframe;

public class Configuration {
	private String controllerNamePath = "com.iisquare.jwframe"; // Controller所在包路径
	private String characterEncoding = "UTF-8"; // 字符集编码
	private String contentType = "text/html;charset=utf-8"; // 页面类型
	private String defaultControllerName = "index"; // 默认控制器名称
	private String defaultActionName = "index"; // 默认方法名称
	private String defaultErrorController = "error"; // 错误处理控制器名称
	private String defaultErrorAction = "index"; // 错误处理方法名称
	private String defaultControllerSuffix = "Controller"; // 控制器后缀
	private String defaultActionSuffix = "Action"; // 方法后缀
	private int allowPathParams = 0; // 是否启用路径参数
	private String templateLoaderPath = "/WEB-INF/template/"; // 模板目录
	private String templateSuffix = ".htm"; // 模板后缀
	private String locale = "zh_CN"; // 本地语言
	private String dateTimeFormat = "yyyy-MM-dd HH:mm:ss"; // 默认日期格式

	public String getControllerNamePath() {
		return controllerNamePath;
	}

	public void setControllerNamePath(String controllerNamePath) {
		this.controllerNamePath = controllerNamePath;
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getDefaultControllerName() {
		return defaultControllerName;
	}

	public void setDefaultControllerName(String defaultControllerName) {
		this.defaultControllerName = defaultControllerName;
	}

	public String getDefaultActionName() {
		return defaultActionName;
	}

	public void setDefaultActionName(String defaultActionName) {
		this.defaultActionName = defaultActionName;
	}

	public String getDefaultErrorController() {
		return defaultErrorController;
	}

	public void setDefaultErrorController(String defaultErrorController) {
		this.defaultErrorController = defaultErrorController;
	}

	public String getDefaultErrorAction() {
		return defaultErrorAction;
	}

	public void setDefaultErrorAction(String defaultErrorAction) {
		this.defaultErrorAction = defaultErrorAction;
	}

	public String getDefaultControllerSuffix() {
		return defaultControllerSuffix;
	}

	public void setDefaultControllerSuffix(String defaultControllerSuffix) {
		this.defaultControllerSuffix = defaultControllerSuffix;
	}

	public String getDefaultActionSuffix() {
		return defaultActionSuffix;
	}

	public void setDefaultActionSuffix(String defaultActionSuffix) {
		this.defaultActionSuffix = defaultActionSuffix;
	}

	public int getAllowPathParams() {
		return allowPathParams;
	}

	public void setAllowPathParams(int allowPathParams) {
		this.allowPathParams = allowPathParams;
	}

	public String getTemplateLoaderPath() {
		return templateLoaderPath;
	}

	public void setTemplateLoaderPath(String templateLoaderPath) {
		this.templateLoaderPath = templateLoaderPath;
	}

	public String getTemplateSuffix() {
		return templateSuffix;
	}

	public void setTemplateSuffix(String templateSuffix) {
		this.templateSuffix = templateSuffix;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getDateTimeFormat() {
		return dateTimeFormat;
	}

	public void setDateTimeFormat(String dateTimeFormat) {
		this.dateTimeFormat = dateTimeFormat;
	}
}
