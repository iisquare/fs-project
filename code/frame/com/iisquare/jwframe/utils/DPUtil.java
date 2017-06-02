package com.iisquare.jwframe.utils;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

/**
 * 
 * DataProcess数据处理类
 *
 */
public class DPUtil {
	
	public static final String regexDouble = "^-?\\d+(\\.\\d)*";
	public static final String regexSafeImplode = "^[\\w_]+$";
	
	/**
	 * null false "" 0 "0" 返回true
	 */
	public static boolean empty(Object object) {
		if(null == object) return true;
		if(object instanceof Boolean) {
			return !(Boolean) object;
		}
		if(object instanceof Collection) {
			return ((Collection<?>) object).isEmpty();
		}
		if(object instanceof Map) {
			return ((Map<?, ?>) object).isEmpty();
		}
		if(object.getClass().isArray()) {
			return 0 == Array.getLength(object);
		}
		String str = object.toString();
		if(str.length() < 1) return true;
		if("0".equals(str)) return true;
		return false;
	}
	
	/**
	 * 获取随机整数字符串，最长为16位
	 */
	public static String random(int length) {
		if(length > 16) length = 16;
		String str = Math.random() + "";
		return str.substring(str.length() - length);
	}
	
	/**
	 * 毫秒转换为格式化日期
	 */
	public static String millisToDateTime(long millis, String format) {
		if(empty(millis)) return null;
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(new Date(millis));
	}
	
	/**
	 * 格式化日期转换为毫秒
	 */
	public static long dateTimeToMillis(Object dateTime, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		try {
			return dateFormat.parse(parseString(dateTime)).getTime();
		} catch (ParseException e) {
			return -1;
		}
	}
	
	/**
	 * 获取当前日期
	 */
	public static String getCurrentDateTime(String format) {
		return millisToDateTime(System.currentTimeMillis(), format);
	}
	
	/**
	 * 获取当前秒数
	 */
	public static int getCurrentSeconds() {
		String str = System.currentTimeMillis() + "";
		str = str.substring(0, 10);
		return parseInt(str);
	}
	
	/**
	 * 转换为int类型
	 */
	public static int parseInt(Object object) {
		return (int) parseDouble(object);
	}
	
	/**
	 * 转换为long类型
	 */
	public static long parseLong(Object object) {
		return (long) parseDouble(object);
	}
	
	/**
	 * 转换为double类型
	 */
	public static double parseDouble(Object object) {
		if(null == object) return 0.0;
		String str = object.toString();
		if("".equals(str)) return 0.0;
		str = getFirstMatcher(regexDouble, str);
		if(null == str) return 0.0;
		return Double.parseDouble(str);
	}
	
	/**
	 * 转换为float类型
	 */
	public static float parseFloat(Object object) {
		if(null == object) return 0.0f;
		String str = object.toString();
		if("".equals(str)) return 0.0f;
		str = getFirstMatcher(regexDouble, str);
		if(null == str) return 0.0f;
		return Float.parseFloat(str);
	}
	
	/**
	 * 转换为String类型
	 */
	public static String parseString(Object object) {
		if(null == object) return "";
		return String.valueOf(object);
	}
	
	/**
	 * 比较两个对象是否相等
	 */
	public static boolean equals(Object object1, Object object2) {
		if(null == object1) {
			if(null == object2) return true;
		} else {
			return object1.equals(object2);
		}
		return false;
	}
	
	/**
	 * 获取正则匹配字符串
	 * @param regex 正则表达式
	 * @param str 匹配字符串
	 * @param bGroup 将捕获组作为结果返回
	 * @return
	 */
	public static List<String> getMatcher(String regex, String str, boolean bGroup) {
		List<String> list = new ArrayList<String>();
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		while(matcher.find()) {
			if(bGroup) {
				int count = matcher.groupCount();
				for(int i = 0; i <= count; i++) {
					list.add(matcher.group(i));
				}
			} else {
				list.add(matcher.group());
			}
		}
		return list;
	}
	
	/**
	 * 获取第一个匹配的字符串
	 */
	public static String getFirstMatcher(String regex, String str) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		while(matcher.find()) {
			return matcher.group();
		}
		return null;
	}
	
	/**
	 * 判断字符串是否与表达式匹配
	 */
	public static boolean isMatcher(String regex, String str) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		return matcher.find();
	}
	
	public static String trim(String str) {
		return trim(str, " ");
	}
	
	/**
	 * 去除字符串两边的指定字符
	 * @param str 源字符串
	 * @param trimStr 需要去除的字符
	 * @return
	 */
	public static String trim(String str, String trimStr) {
		if(null == str) return "";
		String regexLeft = "^(" + trimStr + ")*";
		str = str.replaceFirst(regexLeft, "");
		String regexRight = "(" + trimStr + ")*$";
		str = str.replaceFirst(regexRight, "");
		return str;
	}
	
	public static String trimLeft(String str) {
		return trimLeft(str, "");
	}
	
	/**
	 * 去除字符串左边的指定字符
	 * @param str
	 * @param trimStr
	 * @return
	 */
	public static String trimLeft(String str, String trimStr) {
		if(null == str) return "";
		String regexLeft = "^(" + trimStr + ")*";
		str = str.replaceFirst(regexLeft, "");
		return str;
	}
	
	public static String trimRight(String str) {
		return trimRight(str, "");
	}
	
	/**
	 * 去除字符串右边的指定字符
	 * @param str
	 * @param trimStr
	 * @return
	 */
	public static String trimRight(String str, String trimStr) {
		if(null == str) return "";
		String regexRight = "(" + trimStr + ")*$";
		str = str.replaceFirst(regexRight, "");
		return str;
	}
	
	/**
	 * 采用指定表达式分隔字符串
	 * @param string 带分割字符串
	 * @param splitRegex 表达式
	 * @param trimStr 对子项进行trim操作
	 * @return 分隔后的字符串数组
	 */
	public static String[] explode(String string, String splitRegex, String trimStr, boolean filterEmpty) {
		List<String> list = new ArrayList<String>(0);
		if(empty(string)) {
			return new String[]{};
		}
		for (String str : string.split(splitRegex)) {
			if(filterEmpty && empty(str)) continue ;
			if(null != trimStr) {
				list.add(DPUtil.trim(str));
			} else {
				list.add(str);
			}
		}
		return DPUtil.collectionToStringArray(list);
	}
	
	public static String implode(String split, Object[] array) {
		if(null == array) return "";
		int size = array.length;
		if(1 > size) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < size; i++) {
			Object value = array[i];
			if(null == value) continue;
			if(value instanceof Collection) {
				sb.append(implode(split, collectionToArray((Collection<?>) value)));
			} else if(value instanceof Map) {
				sb.append(implode(split, collectionToArray(((Map<?, ?>) value).values())));
			} else {
				sb.append(value);
			}
			if(i + 1 < size) sb.append(split);
		}
		return sb.toString();
	}
	
	/**
	 * 过滤数组
	 * @param objects 数组
	 * @param bTrim 去除空格
	 * @param bEmpty 去除空值
	 * @param bDuplicate 去重
	 * @return 字符串数组
	 */
	public static String[] filterArray(Object[] array, String wrap, boolean bTrim, boolean bEmpty, boolean bSafe, boolean bDuplicate) {
		if(empty(array)) return new String[]{};
		List<Object> list = new ArrayList<Object>();
		for (Object object : array) {
			String str = parseString(object);
			if(bTrim) str = trim(str);
			if(bEmpty && empty(str)) continue ;
			if(bSafe && null == ValidateUtil.filterRegex(regexSafeImplode, str, bTrim, 0, null, null)) continue ;
			if(bDuplicate && list.contains(str)) continue ;
			list.add(null == wrap ? str : DPUtil.stringConcat(wrap, str, wrap));
		}
		return collectionToStringArray(list);
	}
	
	/**
	 * 将String数组转换为ArrayList
	 * @param stringArray
	 * @return
	 */
	public static ArrayList<String> stringArrayToList(String[] stringArray) {
		if(null == stringArray) return new ArrayList<String>(0);
		return new ArrayList<String>(Arrays.asList(stringArray));
	}
	
	/**
	 * 将Collection转换为String数组
	 */
	public static String[] collectionToStringArray(Collection<?> collection) {
		if(null == collection) {
			return new String[]{};
		}
		String[] stringArray = new String[collection.size()];
		collection.toArray(stringArray);
		return stringArray;
	}
	
	/**
	 * 将Collection转换为Object数组
	 */
	public static Object[] collectionToArray(Collection<?> collection) {
		if(null == collection) {
			return new Object[]{};
		}
		Object[] array = new Object[collection.size()];
		collection.toArray(array);
		return array;
	}
	
	/**
	 * 将String数组转换为Integer数组
	 * @param stringArray
	 * @return
	 */
	public static Integer[] arrayToIntegerArray(Object[] array) {
		Integer[] intArray = new Integer[array.length];
		for(int i = 0; i < array.length; i++) {
			intArray[i] = DPUtil.parseInt(array[i]);
		}
		return intArray;
	}

	/**
	 * 深度复制对象信息
	 */
	public static Object clone(Object object) {
		return JSONObject.fromObject(JSONObject.fromObject(object).toString());
	}
	
	/**
	 * 深度复制Bean信息
	 */
	public static Object clone(Object object, Class<?> beanClass) {
		return JSONObject.toBean((JSONObject) clone(object), beanClass);
	}
	
	/**
	 * 将List转换为Set
	 */
	public static <T> Set<T> listToSet(List<T> list) {
		Set<T> set = new HashSet<T>(0);
		set.addAll(list);
		return set;
	}
	
	/**
	 * 将Set转换为List
	 */
	public static <T> List<T> setToList(Set<T> set) {
		List<T> list = new ArrayList<T>(0);
		list.addAll(set);
		return list;
	}
	
	/**
	 * 将字符串首字母小写
	 */
	public static String lowerCaseFirst(String str) {
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}
	
	/**
	 * 将大写字母转换为下划线加小写字母的形式
	 */
	public static String addUnderscores(String name) {
		StringBuilder buf = new StringBuilder( name.replace('.', '_') );
		for (int i = 1; i < buf.length() - 1; i++) { // 此处需要实时获取长度
			if (
				Character.isLowerCase( buf.charAt(i-1) ) &&
				Character.isUpperCase( buf.charAt(i) ) &&
				Character.isLowerCase( buf.charAt(i+1) )
			) {
				buf.insert(i++, '_');
			}
		}
		return buf.toString().toLowerCase();
	}
	
	/**
	 * 将下划线加小写字母转换为驼峰形式
	 */
	public static String upUnderscores(String name) {
		StringBuilder buf = new StringBuilder(name);
		int length = buf.length();
		for (int i = 1; i < length; i++) {
			if ('_' == buf.charAt(i - 1)) {
				buf.replace(i, i + 1, String.valueOf(buf.charAt(i)).toUpperCase());
			}
		}
		return buf.toString().replaceAll("_", "");
	}
	
	/**
	 * 判断下标是否在数组范围内
	 */
	public static boolean isIndexExist(Object[] array, int index) {
		if(null == array) return false;
		if(index < 0) return false;
		return array.length > index;
	}
	
	/**
	 * 判断下标是否在集合范围内
	 */
	public static boolean isIndexExist(Collection<?> collection, int index) {
		if(null == collection) return false;
		if(index < 0) return false;
		return collection.size() > index;
	}
	
	/**
	 * 判断元素是否包含在数组中
	 */
	public static boolean isItemExist(Object[] array, Object item) {
		if(null == array) return false;
		for (Object object : array) {
			if(DPUtil.equals(item, object)) return true;
		}
		return false;
	}
	
	/**
	 * 判断元素是否包含在集合中
	 */
	public static boolean isItemExist(Collection<?> collection, Object item) {
		if(null == collection) return false;
		Iterator<?> iterator = collection.iterator();
		while (iterator.hasNext()) {
			Object object = iterator.next();
			if(DPUtil.equals(item, object)) return true;
		}
		return false;
	}
	
	/**
	 * 安全获取数组中对应下标的值
	 */
	public static Object getByIndex(Object[] array, int index) {
		if(isIndexExist(array, index)) return array[index];
		return null;
	}
	
	/**
	 * 安全获取集合中对应下标的值
	 */
	public static Object getByIndex(Collection<?> collection, int index) {
		if(isIndexExist(collection, index)) {
			Iterator<?> iterator = collection.iterator();
			for (int i = 0; i < index; i++) {
				iterator.next();
			}
			return iterator.next();
		}
		return null;
	}
	
	/**
	 * 合并多个数组
	 */
	public static Object[] arrayMerge(Object[]... arrays) {
		List<Object> list = new ArrayList<Object>();
		for (Object[] array : arrays) {
			if(null == array) continue ;
			list.addAll(Arrays.asList(array));
		}
		return collectionToArray(list);
	}
	
	/**
	 * 将元素添加到数组末尾
	 */
	public static Object[] arrayPush(Object[] array, Object item) {
		if(null == array) return null;
		return arrayMerge(array, new Object[]{item});
	}

	/**
	 * 将元素添加到数组开头
	 */
	public static Object[] arrayUnShift(Object[] array, Object item) {
		if(null == array) return null;
		return arrayMerge(new Object[]{item}, array);
	}
	
	/**
	 * 将元素从数组中移除
	 */
	public static Object[] arrayRemove(Object[] array, Object item) {
		if(null == array) return null;
		List<Object> list = new ArrayList<Object>(array.length);
		for (Object object : array) {
			if(!equals(item, object)) list.add(object);
		}
		return collectionToArray(list);
	}
	
	/**
	 * 将多个对象连接为字符串
	 */
	public static String stringConcat(Object... objects) {
		StringBuilder sb = new StringBuilder();
		for(Object object : objects) {
			if(null != object) sb.append(object);
		}
		return sb.toString();
	}

	/**
	 * 根据字节宽度截取字符串
	 * @param targetString 目标字符串
	 * @param byteIndex 截取位置
	 * @param suffix 如目标字符串被窃取，则用该字符串作为后缀
	 * @param encoding 字符编码，若为null则默认采用UTF-8格式
	 */
	public static String subStringWithByte(String targetString, int byteIndex, String suffix, String encoding) {
		if(null == targetString) return "";
		if(null == encoding) encoding = "UTF-8";
		try {
			if (targetString.getBytes(encoding).length <= byteIndex) return targetString;
			String temp = targetString;
			int length = targetString.length();
			for (int i = 0; i < length; i++) {
				if (temp.getBytes(encoding).length <= byteIndex) break;
				temp = temp.substring(0, temp.length() - 1);
			}
			return null == suffix ? temp : DPUtil.stringConcat(temp, suffix);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 安全截取字符串，正向从下标0开始，逆向从-1开始
	 */
	public static String subString(String str, int start) {
		if(null == str) return "";
		int length = str.length();
		if(start < 0) start += length; // 转换开始下标到正向位置
		if(start < 0) start = 0; // 处理开始下标正向最小范围溢出
		if(start > length - 1) return ""; // 处理开始下标正向最大范围溢出
		return new String(str.substring(start));
	}
	
	/**
	 * 安全截取字符串，正向从下标0开始，逆向从-1开始
	 */
	public static String subString(String str, int start, int size) {
		if(null == str) return "";
		int length = str.length();
		if(start < 0) start += length; // 转换开始下标到正向位置
		int end = start + size; // 转换结束下标到正向位置
		if(end < start) {
			int temp = start;
			start = end;
			end = temp;
		}
		if(start < 0) start = 0; // 处理开始下标正向最小范围溢出
		if(start > length - 1) return ""; // 处理开始下标正向最大范围溢出
		if(end < 0) return ""; // 处理结束下标正向最小范围溢出
		if(end > length) end = length; // 处理结束下标正向最大范围溢出
		/* beginIndex - 起始索引（包括），endIndex - 结束索引（不包括） */
		return new String(str.substring(start, end));
	}
	
	/**
	 * 获取初始化填充数组
	 */
	public static Object[] getFillArray(int length, Object object) {
		Object[] array = new Object[length];
		Arrays.fill(array, object);
		return array;
	}
	
	/**
	 * 创建HashMap
	 */
	public static Map<Object, Object> buildMap(Object[] keyArray, Object[] valueArray) {
		int length = keyArray.length;
		Map<Object, Object> map = new HashMap<Object, Object>();
		for (int i = 0; i < length; i++) {
			map.put(keyArray[i], valueArray[i]);
		}
		return map;
	}
	
	/**
	 * 解析JSON字符串
	 */
	public static JSONObject parseJSON(String json) {
		if(empty(json)) return null;
		try {
			return JSONObject.fromObject(json);
		} catch (Exception e) {
			return null;
		}
	} 
}
