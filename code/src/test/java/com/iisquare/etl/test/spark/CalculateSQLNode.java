package com.iisquare.etl.test.spark;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;

import com.iisquare.etl.spark.flow.Node;
import com.iisquare.jwframe.utils.DPUtil;

public class CalculateSQLNode extends Node {

	private static final long serialVersionUID = 1L;

	@Override
	public JavaRDD<?> process() throws Exception {
		
		SQLContext sqlContext = SparkSession.builder().config(sparkConf).getOrCreate().sqlContext();
		for (Node node : source) {
			String viewName = node.getProperties().getProperty("alias");
			if(DPUtil.empty(viewName)) viewName = node.getProperties().getProperty("node");
			Dataset<Row> dataset = sqlContext.createDataFrame(node.getResult(), Object.class);
			dataset.show();
			dataset.createTempView(viewName);
		}
		return null;
		/*Dataset<Row> dataset = sqlContext.sql(properties.getProperty("sql"));
		dataset.show();
		return dataset.toJavaRDD();*/
	}

}
