package com.iisquare.jwframe.utils;

/**
 * 安全过滤验证类
 * @author Ouyang <iisquare@163.com>
 *
 */
public class ValidateUtil {
	
	public static final String regexEnglish = "^[a-zA-Z]+$";
	public static final String regexChinese = "^[\u4E00-\u9FA5]+$";
	public static final String regexWord = "^\\w+$";
	public static final String regexSimpleString = "^[\u4E00-\u9FA5\\w]+$";
	public static final String regexEmail = "^\\w+@(\\w+.)+[a-z]{2,3}$";
	public static final String regexDomain = "^(\\w+.)+[a-z]{2,3}$";
	public static final String regexUrl = "^[a-zA-Z]+://(((\\w+.)+[a-z]{2,3})|((\\d{1,3}.){3}\\d{1,3})){1}(/?\\S*)$";
	public static final String regexIPv4 = "^(\\d{1,3}.){3}\\d{1,3}$";
	public static final String regexMobile = "^((\\+86)|(86))?1\\d{10}$";
	public static final String regexPhone = "^((\\d{7,8})|(\\d{4}|\\d{3})-(\\d{7,8})|(\\d{4}|\\d{3})-(\\d{7,8})-(\\d{4}|\\d{3}|\\d{2}|\\d{1})|(\\d{7,8})-(\\d{4}|\\d{3}|\\d{2}|\\d{1}))$";
	public static final String regexIdCard = "^(([1-9]\\d{7}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3})|([1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}([0-9]|[xX]))){1}$";
	public static final String regexPostCode = "^[1-9]\\d{5}(?!\\d)$";
	
	public static boolean isNull(String object, boolean bTrim) {
		return null == object || "".endsWith(bTrim ? DPUtil.trim(object) : object);
	}
	
	public static boolean isEnglish(String object) {
		return DPUtil.isMatcher(regexEnglish, object);
	}
	
	public static boolean isChinese(String object) {
		return DPUtil.isMatcher(regexChinese, object);
	}
	
	public static boolean isWord(String object) {
		return DPUtil.isMatcher(regexWord, object);
	}
	
	public static boolean isSimpleString(String object) {
		return DPUtil.isMatcher(regexSimpleString, object);
	}
	
	public static boolean isEmail(String object) {
		return DPUtil.isMatcher(regexEmail, object);
	}
	
	public static boolean isDomain(String object) {
		return DPUtil.isMatcher(regexDomain, object);
	}
	
	public static boolean isUrl(String object) {
		return DPUtil.isMatcher(regexUrl, object);
	}
	
	public static boolean isIPv4(String object) {
		return DPUtil.isMatcher(regexIPv4, object);
	}
	
	public static boolean isMobile(String object) {
		return DPUtil.isMatcher(regexMobile, object);
	}
	
	public static boolean isPhone(String object) {
		return DPUtil.isMatcher(regexPhone, object);
	}
	
	public static boolean isIdCard(String object) {
		return DPUtil.isMatcher(regexIdCard, object);
	}
	
	public static boolean isPostCode(String object) {
		return DPUtil.isMatcher(regexPostCode, object);
	}
	
	public static Integer filterInteger(Object object, boolean bBound, Integer min, Integer max, Integer defaultValue) {
		if(null == object) return defaultValue;
		int obj = DPUtil.parseInt(object);
		if(null != min && obj < min) return bBound ? min : defaultValue;
		if(null != max && obj > max) return bBound ? max : defaultValue;
		return obj;
	}
	
	public static Long filterLong(Object object, boolean bBound, Long min, Long max, Long defaultValue) {
		if(null == object) return defaultValue;
		long obj = DPUtil.parseLong(object);
		if(null != min && obj < min) return bBound ? min : defaultValue;
		if(null != max && obj > max) return bBound ? max : defaultValue;
		return obj;
	}
	
	public static Double filterDouble(Object object, boolean bBound, Double min, Double max, Double defaultValue) {
		if(null == object) return defaultValue;
		double obj = DPUtil.parseDouble(object);
		if(null != min && obj < min) return bBound ? min : defaultValue;
		if(null != max && obj > max) return bBound ? max : defaultValue;
		return obj;
	}
	
	public static Float filterFloat(Object object, boolean bBound, Float min, Float max, Float defaultValue) {
		if(null == object) return defaultValue;
		float obj = DPUtil.parseFloat(object);
		if(null != min && obj < min) return bBound ? min : defaultValue;
		if(null != max && obj > max) return bBound ? max : defaultValue;
		return obj;
	}
	
	public static String filterLength(String object, Integer min, Integer max, String defaultValue) {
		if(null == object) return defaultValue;
		int obj = object.length();
		if(null != min && obj < min) return defaultValue;
		if(null != max && obj > max) return defaultValue;
		return object;
	}
	
	public static String filterRegex(String pattener, String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		if(null == object) return defaultValue;
		if(bTrim) object = DPUtil.trim(object);
		if(object.length() > 0 && !DPUtil.isMatcher(pattener, object)) return defaultValue;
		return filterLength(object, min, max, defaultValue);
	}
	
	public static String filterEnglish(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexEnglish, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterChinese(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexChinese, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterWord(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexWord, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterSimpleString(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexSimpleString, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterEmail(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexEmail, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterDomain(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexDomain, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterUrl(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexUrl, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterIPv4(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexIPv4, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterMobile(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexMobile, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterPhone(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexPhone, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterIdCard(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexIdCard, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterPostCode(String object, boolean bTrim, Integer min, Integer max, String defaultValue) {
		return filterRegex(regexPostCode, object, bTrim, min, max, defaultValue);
	}
	
	public static String filterDateTime(String object, boolean bTrim, String format, String defaultValue) {
		if(null == object) return defaultValue;
		if(bTrim) object = DPUtil.trim(object);
		long millis = DPUtil.dateTimeToMillis(object, format);
		if(-1 == millis) return defaultValue;
		return DPUtil.millisToDateTime(millis, format);
	}
	
	/**
	 * 判断字符串是否存在于给定的数组中，若不存在则返回默认值
	 */
	public static String filterItem(String object, boolean bTrim, String[] itemArray, String defaultItem) {
		if(null == object) return defaultItem;
		if(bTrim) object = DPUtil.trim(object);
		for (String item : itemArray) {
			if(DPUtil.equals(object, item)) return item;
		}
		return defaultItem;
	}
}
