package com.iisquare.jwframe.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 反射处理类
 * @author Ouyang <iisquare@163.com>
 *
 */
public class ReflectUtil {
	/**
	 * 将实体对象转化为Map
	 * @param object 对象实例
	 * @param bUnderscores 是否添加下划线
	 * @param extendFields 拓充属性数组
	 * @return
	 */
	public static Map<String, Object> convertEntityToMap(Object object, boolean bUnderscores, String[] extendFields) {
		Class<?> instance = object.getClass();
		Field[] field = instance.getDeclaredFields();
		try {
			/* 获取类属性 */
			int length = field.length;
			Object[] fields = new String[length];
			for (int i = 0; i < length; i++) {
				fields[i] = field[i].getName();
			}
			fields = DPUtil.arrayMerge(fields, extendFields); // 拓充属性数组
			/* 获取属性键值对 */
			Map<String, Object> map = new LinkedHashMap<String, Object>(DPUtil.parseInt(fields.length / 0.75f));
			for (Object item : fields) {
				String name = DPUtil.parseString(item);
				name = name.substring(0, 1).toUpperCase() + name.substring(1);
				Method method = instance.getMethod("get" + name);
				Object value = method.invoke(object);
				if(bUnderscores) name = DPUtil.addUnderscores(name);
				map.put(name, value);
			}
			return map;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 获取对象属性值
	 * @param object 对象实例
	 * @param property 属性名称
	 * @return
	 */
	public static Object getPropertyValue(Object object, String property) {
		Class<?> instance = object.getClass();
		try {
			property = property.substring(0, 1).toUpperCase() + property.substring(1);
			Method method = instance.getMethod(DPUtil.stringConcat("get", property));
			return method.invoke(object);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 设置对象属性值
	 * @param object 对象实例
	 * @param property 属性名称
	 * @param parameterTypes 参数类型，若为null值，则从属性值中自动转换类型，此时基础类型也会作为Class处理
	 * @param args 属性值
	 * @return
	 */
	public static Object setPropertyValue(Object object, String property, Class<?>[] parameterTypes, Object[] args) {
		Class<?> instance = object.getClass();
		try {
			property = property.substring(0, 1).toUpperCase() + property.substring(1);
			if(null == parameterTypes && null != args) {
				int length = args.length;
				parameterTypes = new Class<?>[length];
				for (int i = 0; i < length; i++) {
					parameterTypes[i] = args[i].getClass();
				}
			}
			Method method = instance.getMethod(DPUtil.stringConcat("set", property), parameterTypes);
			return method.invoke(object, args);
		} catch (Exception e) {
			return null;
		}
	}
}
