package com.iisquare.jwframe.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.iisquare.jwframe.mvc.MySQLBase;

@Component
@Scope("prototype")
public class DemoDao extends MySQLBase<DemoDao> {

	@Override
	public String tableName() {
		return tablePrefix() + "demo";
	}

	@Override
	public LinkedHashMap<String, Map<String, Object>> columns() {
		LinkedHashMap<String, Map<String, Object>> columns = new LinkedHashMap<>();
		columns.put("id", null); // 主键
		columns.put("parent_id", null); // 父级
		columns.put("name", null); // 名称
		columns.put("status", null); // 状态
		return columns;
	}
	
	public DemoDao self() {
		return this;
	}

	@Override
	protected boolean createTable() {
		return null != executeUpdate(new StringBuilder()
			.append("CREATE TABLE `").append(tableName()).append("` (")
			.append("	`id` int(11) NOT NULL AUTO_INCREMENT,")
			.append("	`parent_id` int(11) NOT NULL DEFAULT '0',")
			.append("	`name` varchar(64) NOT NULL,")
			.append("	`status` tinyint(4) NOT NULL DEFAULT '0',")
			.append("	PRIMARY KEY (`id`),")
			.append("	KEY `parent_id` (`parent_id`)")
			.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8;")
			.toString());
	}

}
