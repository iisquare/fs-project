package com.iisquare.jwframe.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.iisquare.jwframe.mvc.MySQLBase;

@Component
@Scope("prototype")
public class JobDao extends MySQLBase<JobDao> {

	@Override
	public String tableName() {
		return tablePrefix() + "job";
	}

	@Override
	public LinkedHashMap<String, Map<String, Object>> columns() {
		LinkedHashMap<String, Map<String, Object>> columns = new LinkedHashMap<>();
		columns.put("id", null); // 主键
		columns.put("flow_id", null); // 流程图主键
		columns.put("flow_content", null); // 流程图快照
		columns.put("status", null); // 当前状态
		columns.put("trigger_time", null); // 任务触发时间
		columns.put("dispatch_time", null); // 调度开始时间
		columns.put("dispatched_time", null); // 调度完成时间
		columns.put("complete_time", null); // 处理完成事件
		return columns;
	}

}
