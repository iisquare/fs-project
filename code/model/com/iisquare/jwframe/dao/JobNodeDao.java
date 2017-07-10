package com.iisquare.jwframe.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.iisquare.jwframe.mvc.MySQLBase;

@Component
@Scope("prototype")
public class JobNodeDao extends MySQLBase<JobNodeDao> {

	@Override
	public String tableName() {
		return tablePrefix() + "job_node";
	}

	@Override
	public LinkedHashMap<String, Map<String, Object>> columns() {
		LinkedHashMap<String, Map<String, Object>> columns = new LinkedHashMap<>();
		columns.put("job_id", null); // 作业主键
		columns.put("node_id", null); // 节点标识
		columns.put("content", null); // 附加信息
		columns.put("start_time", null); // 开始时间
		columns.put("end_time", null); // 完成时间
		return columns;
	}

}
