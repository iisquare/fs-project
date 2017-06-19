package com.iisquare.etl.test.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Row;
import org.elasticsearch.spark.rdd.EsSpark;

import com.iisquare.etl.spark.flow.Node;
import com.iisquare.jwframe.utils.DPUtil;

public class WriteElasticsearchNode extends Node {

	private static final long serialVersionUID = 1L;

	@Override
	public JavaRDD<Row> process() {
		System.out.println(this.getClass());
		SparkConf sparkConf = this.sparkConf.clone();
		sparkConf.set("es.nodes", properties.getProperty("nodes"));
		sparkConf.set("es.port", properties.getProperty("port"));
		sparkConf.set("es.index.auto.create", properties.getProperty("autoCreate"));
		String user = properties.getProperty("user");
		if(!DPUtil.empty(user)) {
			sparkConf.set("es.net.http.auth.user", user);
			sparkConf.set("es.net.http.auth.pass", properties.getProperty("pass"));
		}
		for (Node node : source) {
			if(null == node.getResult()) continue;
			EsSpark.saveToEs(node.getResult().rdd(), properties.getProperty("collection"));
		}
		return null;
	}

}
