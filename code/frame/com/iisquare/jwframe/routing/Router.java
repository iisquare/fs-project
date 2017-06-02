package com.iisquare.jwframe.routing;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.iisquare.jwframe.Configuration;
import com.iisquare.jwframe.exception.ApplicationException;
import com.iisquare.jwframe.mvc.ControllerBase;
import com.iisquare.jwframe.utils.DPUtil;

public class Router {
	
	private static Map<String, ArrayList<RouteURI>> routes = new Hashtable<>();
    private static LinkedHashMap<String, String> domains = new LinkedHashMap<>();
    private WebApplicationContext wac;
    private Configuration configuration;
    private String appUri, rootPath;
    private HttpServletRequest request;
	private HttpServletResponse response;
	private Logger logger = Logger.getLogger(getClass().getName());
    
    static {
    	domains.put("*", "frontend");
    	init(domains);
    }
    
    public Router(String appUri, String rootPath, HttpServletRequest request,
			HttpServletResponse response) {
		this.appUri = appUri;
		this.rootPath = rootPath;
		this.request = request;
		this.response = response;
		wac = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
		configuration = wac.getBean(Configuration.class);
	}

	public synchronized static void init(LinkedHashMap<String, String> customDomains) {
    	domains = new LinkedHashMap<>(); // 避免并发读写
    	for (Entry<String, String> entry : customDomains.entrySet()) {
    		String domain = "^" + entry.getKey().replaceAll("\\.", "\\\\.").replaceAll("\\*", ".+") + "$";
    		domains.put(domain, entry.getValue());
    	}
    }
    
    public static boolean get(String module, String uri, Generator action) {
        return addRoute(module, new String[] {"GET", "HEAD"}, uri, action);
    }
    
    public static boolean post(String module, String uri, Generator action) {
        return addRoute(module, new String[] {"POST"}, uri, action);
    }
    
    public static boolean put(String module, String uri, Generator action) {
        return addRoute(module, new String[] {"PUT"}, uri, action);
    }
    
    public static boolean patch(String module, String uri, Generator action) {
        return addRoute(module, new String[] {"PATCH"}, uri, action);
    }
    
    public static boolean delete(String module, String uri, Generator action) {
        return addRoute(module, new String[] {"DELETE"}, uri, action);
    }
    
    public static boolean options(String module, String uri, Generator action) {
        return addRoute(module, new String[] {"OPTIONS"}, uri, action);
    }
    
    public static boolean any(String module, String uri, Generator action) {
        String[] verbs = new String[] {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE"};
        return addRoute(module, verbs, uri, action);
    }
    
    public static boolean match(String module, String[] methods, String uri, Generator action) {
        return addRoute(module, methods, uri, action);
    }
    
    protected synchronized static boolean addRoute(String module, String[] methods, String uri, Generator action) {
    	ArrayList<RouteURI> list = routes.get(module);
    	if(null == list) {
    		list = new ArrayList<>();
    		routes.put(module, list);
    	}
    	return list.add(new RouteURI(methods, uri, action));
    }
    
    public String parseModule(String host) {
    	for (Entry<String, String> entry : domains.entrySet()) {
    		if(host.matches(entry.getKey())) return entry.getValue();
    	}
        return null;
    }
    
    public RouteAction parseRoute(String module, String uri) {
    	ArrayList<RouteURI> list = routes.get(module);
    	if(null == list) return null;
        for (RouteURI route : list) {
        	List<String> matches = DPUtil.getMatcher(route.getUri(), uri, true);
            if(matches.isEmpty()) continue ;
            Generator generator = route.getAction();
            if(null == generator) return null;
            return generator.call(DPUtil.collectionToStringArray(matches.subList(1, matches.size())));
        }
        return null;
    }
    
    public RouteAction conventionRoute(String uri) {
        RouteAction route = new RouteAction(
        		configuration.getDefaultControllerName(), configuration.getDefaultActionName(), null);
        uri = DPUtil.trim(uri, "/");
        if("".equals(uri)) return route;
        String[] uriArray = DPUtil.explode(uri, "/", null, false);
        route.setControllerName(uriArray[0]);
        if(1 == uriArray.length) return route;
        route.setActionName(uriArray[1]);
        if(2 == uriArray.length) return route;
        if(DPUtil.empty(configuration.getAllowPathParams()) || uriArray.length % 2 != 0) return null;
        Map<String, String[]> params = new LinkedHashMap<>();
        for (int i = 2; i < uriArray.length; i += 2) {
        	params.put(uriArray[i], new String[] {uriArray[i + 1]});
        }
        route.setParams(params);
        return route;
    }
    
    private Exception invoke(String module, RouteAction route, Object arg) {
    	if(null == route) return new ApplicationException("no route matches!");
    	String controllerName = route.getControllerName();
		String actionName = route.getActionName();
    	Class<?> controller;
		try {
			controller = Class.forName(configuration.getControllerNamePath()
					+ "." + module + ".controller."
					+ controllerName.substring(0, 1).toUpperCase()
					+ controllerName.substring(1)
					+ configuration.getDefaultControllerSuffix());
			ControllerBase instance = (ControllerBase) wac.getBean(controller);
	    	instance.setWebApplicationContext(wac);
			int port = request.getServerPort();
			String appUrl = request.getScheme() + "://" + request.getServerName();
			if (80 != port) appUrl += ":" + port;
			String appPath = appUrl + appUri;
			instance.setAppUrl(appUrl);
			instance.setAppUri(appUri);
			instance.setAppPath(appPath);
			instance.setRootPath(rootPath);
			instance.setModuleName(module);
			instance.setControllerName(controllerName);
			instance.setActionName(actionName);
			instance.setRequest(request);
			instance.setResponse(response);
			Map<String, String[]> paramsMap = request.getParameterMap();
			Map<String, String[]> params = route.getParams();
			if(null == params) params = new LinkedHashMap<>();
			for (Entry<String, String[]> entry : paramsMap.entrySet()) {
				params.put(entry.getKey(), entry.getValue());
			}
			instance.setParams(params);
			instance.setAssign(new LinkedHashMap<String, Object>());
			Object initVal = instance.init();
			if(null != initVal) return new ApplicationException("initError");
			Object actionVal;
			if(null == arg) {
				actionVal = controller.getMethod(actionName
						+ configuration.getDefaultActionSuffix()).invoke(instance);
			} else {
				actionVal = controller.getMethod(actionName
						+ configuration.getDefaultActionSuffix(), Exception.class).invoke(instance, arg);
			}
			Object destroyVal = instance.destroy(actionVal);
			if (null != destroyVal) return new ApplicationException("destroyError");
		} catch (Exception e) {
			return new Exception("Route:controller["
					+ route.getControllerName() + "] - action[" + route.getActionName() + "]", e);
		}
    	return null;
    }
    
    public Object dispatch() {
        // 模块检测
        String module = parseModule(request.getServerName());
        if(null == module) return new ApplicationException("no module matches!");
        // URI检测
        String uri = request.getRequestURI();
        if(!uri.startsWith(appUri)) return new ApplicationException("app uri error!");
        uri = "/" + uri.substring(appUri.length());
        // 默认编码设置
        String characterEncoding = configuration.getCharacterEncoding();
        try {
			request.setCharacterEncoding(characterEncoding);
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
		}
		response.setCharacterEncoding(characterEncoding);
		response.setContentType(configuration.getContentType());
        // 自定义路由检测
        RouteAction route = parseRoute(module, uri);
        // 约定路由检测
        if(null == route) route = conventionRoute(uri);
        // 执行路由
        Object retVal = invoke(module, route, null);
        if(null == retVal) return null;
        // 执行路由失败，调用错误处理
        route = new RouteAction(configuration.getDefaultErrorController(), configuration.getDefaultErrorAction(), null);
        return invoke(module, route, retVal);
    }
}
