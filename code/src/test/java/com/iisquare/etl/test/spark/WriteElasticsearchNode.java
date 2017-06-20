package com.iisquare.etl.test.spark;

import java.util.HashMap;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import com.iisquare.etl.spark.flow.Node;
import com.iisquare.jwframe.utils.DPUtil;

public class WriteElasticsearchNode extends Node {

	private static final long serialVersionUID = 1L;

	@Override
	public JavaRDD<Map<String, Object>> call() {
		Map<String, String> config = new HashMap<>();
		config.put("es.nodes", properties.getProperty("nodes"));
		config.put("es.port", properties.getProperty("port"));
		config.put("es.index.auto.create", properties.getProperty("autoCreate"));
		String user = properties.getProperty("user");
		if(!DPUtil.empty(user)) {
			config.put("es.net.http.auth.user", user);
			config.put("es.net.http.auth.pass", properties.getProperty("pass"));
		}
		String mappingId = properties.getProperty("mappingId");
		if(!DPUtil.empty(mappingId)) config.put("es.mapping.id", mappingId);
		for (Node node : source) {
			if(null == node.getResult()) continue;
			JavaEsSpark.saveToEs(node.getResult(), properties.getProperty("collection"), config);
		}
		return null;
	}

}
