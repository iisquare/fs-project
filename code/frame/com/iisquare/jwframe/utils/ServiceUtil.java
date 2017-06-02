package com.iisquare.jwframe.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用业务辅助处理类
 * @author Ouyang <iisquare@163.com>
 *
 */
public class ServiceUtil {

	public static final String relInfoPostfix = "_info";
	
	/**
	 * 填充关联记录
	 */
/*	public static Map<String, Object> fillRelations(Map<String, Object> map,
			DaoBase<?> relationDao, String[] relationFields, Object[] fillFields, String fieldPostfix) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(1);
		list.add(map);
		list = fillRelations(list, relationDao, relationFields, fillFields, fieldPostfix);
		return list.get(0);
	}*/
	
	/**
	 * 填充关联记录
	 */
/*	public static List<Map<String, Object>> fillRelations(List<Map<String, Object>> list,
			DaoBase<?> relationDao, String[] relationFields, Object[] fillFields, String fieldPostfix) {
		String primaryKey = relationDao.getIdName();
		List<Object> idList = ServiceUtil.getFieldValues(list, relationFields);
		if(DPUtil.empty(fillFields)) {
			fillFields = new String[]{"*"};
		} else {
			if(!DPUtil.isItemExist(fillFields, primaryKey)) {
				fillFields = DPUtil.arrayUnShift(fillFields, primaryKey);
			}
		}
		List<Map<String, Object>> settingList = relationDao.getByIds(
				DPUtil.implode(",", fillFields), DPUtil.collectionToArray(idList));
		Map<Object, Map<String, Object>> indexMap = ServiceUtil.indexMapList(settingList, primaryKey);
		if(null == fieldPostfix) fieldPostfix = relInfoPostfix;
		for (Map<String, Object> item : list) {
			for (String field : relationFields) {
				item.put(DPUtil.stringConcat(field, fieldPostfix), indexMap.get(item.get(field)));
			}
		}
		return list;
	}*/
	
	/**
	 * 填充属性信息
	 */
	public static Map<String, Object> fillProperties(Map<String, Object> map,
			Object entity, String[] referProperties, String[] fillProperties, boolean bUnderscores) {
		int length = referProperties.length;
		for (int i = 0; i < length; i++) {
			String referProperty = referProperties[i];
			if(null != referProperty) { // 注入引用属性
				ReflectUtil.setPropertyValue(entity, referProperty, null,
						new Object[]{map.get(bUnderscores ? DPUtil.addUnderscores(referProperty) : referProperty)});
			}
			String fillProperty = fillProperties[i];
			String key = bUnderscores ? DPUtil.addUnderscores(fillProperty) : fillProperty;
			Object value = ReflectUtil.getPropertyValue(entity, fillProperty);
			map.put(key, value);
		}
		return map;
	}
	
	/**
	 * 填充属性信息
	 */
	public static List<Map<String, Object>> fillProperties(List<Map<String, Object>> list,
			Object entity, String[] referProperties, String[] fillProperties, boolean bUnderscores) {
		for (Map<String, Object> map : list) {
			fillProperties(map, entity, referProperties, fillProperties, bUnderscores); // 此处map为内存地址（传址）
		}
		return list;
	}
	
	/**
	 * 填充属性信息，referMaps必须是Map<String, String>类型
	 */
	public static Map<String, Object> fillFields(Map<String, Object> map,
			Object[] referFields, Map<?, ?>[] referMaps, String fieldPostfix) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(1);
		list.add(map);
		list = fillFields(list, referFields, referMaps, fieldPostfix);
		return list.get(0);
	}
	
	/**
	 * 填充属性信息，referMaps必须是Map<String, String>类型
	 */
	public static List<Map<String, Object>> fillFields(List<Map<String, Object>> list,
			Object[] referFields, Map<?, ?>[] referMaps, String fieldPostfix) {
		if(null == fieldPostfix) fieldPostfix = "_text";
		int length = referFields.length;
		for (Map<String, Object> map : list) {
			for (int i = 0; i < length; i++) {
				Object referProperty = referFields[i];
				String key = DPUtil.stringConcat(referProperty, fieldPostfix);
				Object value = referMaps[i].get(DPUtil.parseString(map.get(referProperty)));
				map.put(key, value);
			}
		}
		return list;
	}
	
	/**
	 * 填充关联记录对应的字段值
	 */
/*	public static Map<String, Object> fillFields(Map<String, Object> map,
			DaoBase<?> daoBase, String[] relFields, String[] fields) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(1);
		list.add(map);
		list = fillFields(list, daoBase, relFields, fields);
		return list.get(0);
	}*/
	
	/**
	 * 填充关联记录对应的字段值
	 */
/*	public static List<Map<String, Object>> fillFields(List<Map<String, Object>> list,
			DaoBase<?> daoBase, String[] relFields, String[] fields) {
		List<Object> ids = getFieldValues(list, relFields);
		if(DPUtil.empty(ids)) return list;
		 避免在循环中查询数据库 
		List<Map<String, Object>> infoList = daoBase.getByIds("*", DPUtil.collectionToArray(ids));
		Map<Object, Map<String, Object>> indexMap = indexMapList(infoList, daoBase.getPrimaryKey());
		int length = relFields.length;
		for (Map<String, Object> item : list) {
			for (int i = 0; i < length; i++) {
				String relField = relFields[i];
				String field = fields[i];
				String key = DPUtil.stringConcat(relField, "_", field);
				Object value = null;
				Map<String, Object> map = indexMap.get(item.get(relField));
				if(null != map) {
					value = map.get(field);
				}
				item.put(key, value);
			}
		}
		return list;
	}*/
	
	/**
	 * 获取对应字段的值列表
	 */
	public static List<Object> getPropertyValues(List<?> list, String... properties) {
		List<Object> valueList = new ArrayList<Object>(list.size());
		for (Object object : list) {
			for (String property : properties) {
				Object value = ReflectUtil.getPropertyValue(object, property);
				if(null == value) continue;
				valueList.add(value);
			}
		}
		return valueList;
	}
	
	/**
	 * 获取对应字段的值列表
	 */
	public static List<Object> getFieldValues(List<Map<String, Object>> list, String... fields) {
		List<Object> valueList = new ArrayList<Object>(list.size());
		for (Map<String, Object> item : list) {
			for (String field : fields) {
				Object value = item.get(field);
				if(null == value) continue;
				valueList.add(value);
			}
		}
		return valueList;
	}
	
	/**
	 * 将List数据格式化为以对应字段值为下标的Map
	 */
	public static Map<Object, Object> indexObjectList(List<?> infoList, String property) {
		Map<Object, Object> map = new HashMap<Object, Object>(DPUtil.parseInt(infoList.size() / 0.75f));
		for (Object item : infoList) {
			map.put(ReflectUtil.getPropertyValue(item, property), item);
		}
		return map;
	}
	
	/**
	 * 将List数据格式化为以对应字段值为下标的Map
	 */
	public static Map<Object, List<Object>> indexesObjectList(List<Object> list, String property) {
		Map<Object, List<Object>> map = new HashMap<Object, List<Object>>(DPUtil.parseInt(list.size() / 0.75f));
		for (Object item : list) {
			Object key = ReflectUtil.getPropertyValue(item, property);
			if(null == key) continue;
			List<Object> subList = map.get(key);
			if(null == subList) {
				subList = new ArrayList<Object>();
			}
			subList.add(item);
			map.put(key, subList);
		}
		return map;
	}
	
	/**
	 * 将List数据格式化为以对应字段值为下标的Map
	 */
	public static Map<Object, Map<String, Object>> indexMapList(List<Map<String, Object>> list, String field) {
		Map<Object, Map<String, Object>> map = new HashMap<Object, Map<String, Object>>(DPUtil.parseInt(list.size() / 0.75f));
		for (Map<String, Object> item : list) {
			map.put(item.get(field), item);
		}
		return map;
	}
	
	/**
	 * 将List数据格式化为以对应字段值为下标的Map
	 */
	public static Map<Object, List<Map<String, Object>>> indexesMapList(List<Map<String, Object>> list, String field) {
		Map<Object, List<Map<String, Object>>> map = new HashMap<Object, List<Map<String, Object>>>(DPUtil.parseInt(list.size() / 0.75f));
		for (Map<String, Object> item : list) {
			Object key = item.get(field);
			if(null == key) continue;
			List<Map<String, Object>> subList = map.get(key);
			if(null == subList) {
				subList = new ArrayList<Map<String, Object>>();
			}
			subList.add(item);
			map.put(key, subList);
		}
		return map;
	}
	
	/**
	 * 格式化层级关系
	 */
	public static List<Map<String, Object>> formatRelation(List<Map<String, Object>> list, Object root) {
		return formatRelation(list, "id", "parent_id", "children", root);
	}
	
	/**
	 * 格式化层级关系
	 * @param list 记录列表
	 * @param primaryKey 主键名称
	 * @param parentKey 上级节点外键名称
	 * @param childrenKey 下级节点列表名称
	 * @param root 指定根节点（返回信息不含该值对应的记录）
	 * 		当root为null时，采用所有上级记录为空的节点（含）作为根节点
	 * @return
	 */
	public static List<Map<String, Object>> formatRelation(List<Map<String, Object>> list,
			String primaryKey, String parentKey, String childrenKey, Object root) {
		/* 转换为以parentKey为下标的映射关系列表 */
		Map<Object, List<Map<String, Object>>> parentMap = new HashMap<Object, List<Map<String, Object>>>();
		for (Map<String, Object> item : list) {
			Object parentValue = item.get(parentKey);
			List<Map<String, Object>> listSub = parentMap.get(parentValue);
			if(null == listSub) {
				listSub = new ArrayList<Map<String, Object>>();
			}
			listSub.add(item);
			parentMap.put(parentValue, listSub);
		}
		if(null != root) return processFormatRelation(parentMap, primaryKey, childrenKey, root);
		List<Map<String, Object>> rootList = processRelationRoot(list, primaryKey, parentKey); // 获取所有上级为空的节点
		for (Map<String, Object> rootMap : rootList) { // 处理下级节点
			rootMap.put(childrenKey, processFormatRelation(parentMap, primaryKey, childrenKey, rootMap.get(primaryKey)));
		}
		return rootList;
	}
	
	/**
	 * 获取上级记录为空的节点列表
	 */
	public static List<Map<String, Object>> processRelationRoot(
			List<Map<String, Object>> list, String primaryKey, String parentKey) {
		List<Map<String, Object>> rootList = new ArrayList<Map<String, Object>>();
		Map<Object, Map<String, Object>> indexMap = indexMapList(list, primaryKey);
		for (Map<String, Object> item : list) {
			if(null != indexMap.get(item.get(parentKey))) continue ;
			rootList.add(item);
		}
		return rootList;
	}
	
	/**
	 * 以root为根节点，逐层向下处理层级关系
	 */
	public static List<Map<String, Object>> processFormatRelation(
			Map<Object, List<Map<String, Object>>> parentMap, String primaryKey, String childrenKey, Object root) {
		List<Map<String, Object>> list = parentMap.get(root);
		if(null == list) {
			return new ArrayList<Map<String, Object>>();
		}
		for(Map<String, Object> map : list) {
			map.put(childrenKey, processFormatRelation(parentMap, primaryKey, childrenKey, map.get(primaryKey)));
		}
		return list;
	}
}
