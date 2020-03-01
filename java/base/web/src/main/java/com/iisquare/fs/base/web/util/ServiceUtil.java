package com.iisquare.fs.base.web.util;

import com.iisquare.fs.base.core.util.ReflectUtil;

import java.util.*;

/**
 * 通用业务辅助处理类
 * @author Ouyang <iisquare@163.com>
 *
 */
public class ServiceUtil {

	public static <T> List<T> formatRelation(List<?> data, Class<T> requiredType, String parentKey, Object parentValue, String idKey, String childrenKey) {
		List<T> list = new ArrayList<>();
		for(Object item : data) {
			if(!parentValue.equals(ReflectUtil.getPropertyValue(item, parentKey))) continue;
			list.add((T) item);
			List<T> children = formatRelation(data, requiredType, parentKey, ReflectUtil.getPropertyValue(item, idKey), idKey, childrenKey);
			if(children.size() < 1) continue;
			ReflectUtil.setPropertyValue(item, childrenKey, new Class[] {List.class}, new Object[]{children});
		}
		return list;
	}

	public static void fillFields(List<Map<String, Object>> list, String[] froms, String[] tos, Map<?, ?> ...maps) {
		if(null == list) return;
		for (Map<String, Object> item : list) {
			for (int i = 0; i < froms.length; i++) {
				item.put(tos[i], maps[i].get(item.get(froms[i])));
			}
		}
	}

	public static void fillProperties(List<?> list, String[] froms, String[] tos, Map<?, ?> ...maps) {
		if(null == list) return;
		for (Object item : list) {
			for (int i = 0; i < froms.length; i++) {
				ReflectUtil.setPropertyValue(item, tos[i], null, new Object[] {
					maps[i].get(ReflectUtil.getPropertyValue(item, froms[i]))
				});
			}
		}
	}

	/**
	 * 获取对应字段的值列表
	 */
	public static <T> Set<T> getPropertyValues(List<?> list, Class<T> requiredType, String... properties) {
		Set<T> valueList = new HashSet<>();
		for (Object object : list) {
			for (String property : properties) {
				Object value = ReflectUtil.getPropertyValue(object, property);
				if(null == value) continue;
				valueList.add((T) value);
			}
		}
		return valueList;
	}

	public static <T> Set<T> getFieldValues(List<Map<String, Object>> list, Class<T> requiredType, String... fields) {
		Set<T> valueList = new HashSet<>();
		for (Map<String, Object> item : list) {
			for (String field : fields) {
				Object value = item.get(field);
				if(null == value) continue;
				valueList.add((T) value);
			}
		}
		return valueList;
	}

	/**
	 * 将List数据格式化为以对应字段值为下标的Map
	 */
	public static <K, V> Map<K, V> indexObjectList(List<?> list, Class<K> kType, Class<V> vType, String property) {
		Map<K, V> map = new LinkedHashMap<>();
		for (Object item : list) {
			map.put((K) ReflectUtil.getPropertyValue(item, property), (V) item);
		}
		return map;
	}

	/**
	 * 将List数据格式化为以对应字段值为下标的Map
	 */
	public static Map<?, ?> indexesObjectList(List<?> list, String property) {
		Map<Object, List<Object>> map = new LinkedHashMap<>();
		for (Object item : list) {
			Object key = ReflectUtil.getPropertyValue(item, property);
			if(null == key) continue;
			List<Object> subList = map.get(key);
			if(null == subList) {
				subList = new ArrayList<>();
			}
			subList.add(item);
			map.put(key, subList);
		}
		return map;
	}

	/**
	 * 将List数据格式化为以对应字段值为下标的Map
	 */
	public static Map<?, ?> indexMapList(List<Map<String, Object>> list, String field) {
		Map<Object, Map<String, Object>> map = new LinkedHashMap<>();
		for (Map<String, Object> item : list) {
			map.put(map.get(field), item);
		}
		return map;
	}

	/**
	 * 将List数据格式化为以对应字段值为下标的Map
	 */
	public static Map<?, ?> indexesMapList(List<Map<String, Object>> list, String field) {
		Map<Object, List<Map<String, Object>>> map = new LinkedHashMap<>();
		for (Map<String, Object> item : list) {
			Object key = item.get(field);
			if(null == key) continue;
			List<Map<String, Object>> subList = map.get(key);
			if(null == subList) {
				subList = new ArrayList<>();
			}
			subList.add(item);
			map.put(key, subList);
		}
		return map;
	}

}
