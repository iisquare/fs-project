package com.iisquare.jwframe.mvc;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public abstract class ServiceBase {

	protected Map<String, Object> lastError;
	
	public <T> T setLastError(int code, Object message, Object data, T result) {
		lastError = new LinkedHashMap<>();
		lastError.put("code", code);
		lastError.put("message", message);
		lastError.put("data", data);
		return result;
	}
	
	public Map<String, Object> getLastError() {
		return lastError;
	}
	
	public boolean hasError() {
		return null != lastError;
	}	
	
}
