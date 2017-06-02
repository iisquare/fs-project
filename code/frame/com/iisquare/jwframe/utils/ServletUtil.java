package com.iisquare.jwframe.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet操作类
 */
public class ServletUtil {
	
	public static final String cookieEncoding = "UTF-8";
	public static final String regexParameterMapKey = "((?<!\\[)[^\\[\\]]+(?!\\])|(?<=\\[)[^\\[\\]]*(?=\\]))";
	
	/**
	 * 解析ParameterMap，将中括号[]中的字符串转换为下标
	 * 下标支持非中括号[]的任意字符，包括空格等
	 * 若存在多个相同的下标（以中括号[]标识的数组除外），默认取最后一个下标对应的值
	 * @param parameterMap 参数Map
	 * @return
	 */
	public static Map<String, Object> parseParameterMap(Map<String, String[]> parameterMap) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			List<String> keys = DPUtil.getMatcher(regexParameterMapKey, entry.getKey(), true);
			generateParameterMap(map, keys, entry.getValue(), 0, keys.size());
		}
		return map;
	}
	
	/**
	 * 按照KV形式，递归生成ParameterMap
	 * @param map 当前层级的LinkedHashMap<String, Object>
	 * @param keyList 下标列表
	 * @param valueArray 下标对应值
	 * @param index 下标当前位置
	 * @param length 处理深度
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> generateParameterMap(Map<String, Object> map,
			List<String> keyList, String[] valueArray, int index, int length) {
		int indexNext = index + 1;
		String key = keyList.get(index);
		if(indexNext >= length) { // 当前为最终位置，不存在下级元素
			map.put(key, valueArray.length > 0 ? valueArray[valueArray.length - 1] : ""); // 默认取最后一个值
			return map;
		}
		String keyNext = keyList.get(indexNext); // 存在下级元素
		if(0 == keyNext.length()) { // 下级元素为[]数组形式，应为最终位置
			map.put(key, valueArray);
			return map;
		}
		/* 下级元素为KV形式，继续递归处理 */
		Map<String, Object> subMap = (Map<String, Object>) map.get(key);
		if(null == subMap) subMap = new LinkedHashMap<String, Object>(); // 初始化下级Map
		map.put(key, generateParameterMap(subMap, keyList, valueArray, indexNext, length));
		return map;
	}
	
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String key, String value, int maxAge) throws UnsupportedEncodingException {
		if(null != value) value = URLEncoder.encode(value, cookieEncoding);
		Cookie cookie = new Cookie(key, value);
		String host = request.getHeader("host");
		if(host.indexOf(":") > -1) {
			host = host.split(":")[0];
		}
		cookie.setDomain(host);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}
	
	public static String getCookie(HttpServletRequest request, String key) throws UnsupportedEncodingException {
		Cookie cookies[] = request.getCookies();
		if(null == cookies) return null;
		for(Cookie cookie : cookies) {
			if(key.equals(cookie.getName())) return URLDecoder.decode(cookie.getValue(), cookieEncoding);
		}
		return null;
	}
	
	public static void setSession(HttpServletRequest request, Map<String, Object> map) {
		HttpSession session = request.getSession();
		for(Map.Entry<String, Object> item : map.entrySet()) {
			session.setAttribute(item.getKey(), item.getValue());
		}
	}
	
	public static void setSession(HttpServletRequest request, String key, Object value) {
		HttpSession session = request.getSession();
		session.setAttribute(key, value);
	}
	
	public static Map<String, Object> getSession(HttpServletRequest request) {
		HttpSession session = request.getSession();
		Map<String, Object> map = new HashMap<String, Object>();
		Enumeration<?> e = session.getAttributeNames();
		while(e.hasMoreElements()) {
			String key = (String) e.nextElement();
			map.put(key, session.getAttribute(key));
		}
		return map;
	}
	
	public static Object getSession(HttpServletRequest request, String key) {
		HttpSession session = request.getSession();
		return session.getAttribute(key);
	}
	
	public static Map<String, Object> getSessionMap(HttpServletRequest request) {
		HttpSession session = request.getSession();
		Enumeration<String> enumeration = session.getAttributeNames();
		Map<String, Object> map = new HashMap<String, Object>();
		while (enumeration.hasMoreElements()) {
			String name = enumeration.nextElement().toString();
			map.put(name, session.getAttribute(name));
		}
		return map;
	}
	
	public static void invalidateSession(HttpServletRequest request) {
		HttpSession session = request.getSession();
		session.invalidate();
	}
	
	/**
	 * 获取客户端IP地址
	 */
	public static String getRemoteAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}
	
	/**
	 * 获取项目物理路径
	 * @param request
	 * @return
	 */
	public static String getWebRoot(HttpServletRequest request) {
		String webRoot = request.getSession().getServletContext().getRealPath("/");
		return webRoot.substring(0, webRoot.length() - 1);
	}
	
	/**
	 * 获取项目访问地址
	 * @param request
	 * @param bWithDomain 是否携带域名地址
	 * @return
	 */
	public static String getWebUrl(HttpServletRequest request, boolean bWithDomain) {
		StringBuilder sb = new StringBuilder();
		if(bWithDomain) {
			sb.append(request.getScheme()).append("://").append(request.getServerName());
			if(80 != request.getServerPort()) sb.append(":").append(request.getServerPort());
		}
		sb.append(request.getContextPath());
		return sb.toString();
	}
	
	/**
	 * 获取完整请求地址和参数
	 * @param request
	 * @param bWithWebUrl 是否携带项目地址
	 * @param bWithQuery 是否携带请求参数
	 * @return
	 */
	public static String getFullUrl(HttpServletRequest request, boolean bWithWebUrl, boolean bWithQuery) {
		String requestUrl = request.getRequestURL().toString();
		if(bWithQuery) {
			String queryString = request.getQueryString();
			if(null != queryString) requestUrl = DPUtil.stringConcat(requestUrl, "?", queryString);
		}
		if(!bWithWebUrl) requestUrl = requestUrl.substring(getWebUrl(request, true).length());
		return requestUrl;
	}
	
	/**
	 * 获取目录分隔符
	 * @param request
	 * @return
	 */
	public static String getDirectorySeparator(HttpServletRequest request) {
		String webRoot = getWebRoot(request);
		if(webRoot.startsWith("/")) return "/";
		return "\\";
	}
}
