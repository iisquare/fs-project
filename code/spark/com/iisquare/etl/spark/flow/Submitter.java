package com.iisquare.etl.spark.flow;

import org.apache.spark.deploy.SparkSubmit;

import com.iisquare.etl.spark.config.Configuration;

public class Submitter {

	/**
	 * 参照：http://spark.apache.org/docs/latest/submitting-applications.html
	 */
	public static void submit(String json) throws Exception {
		Configuration config = Configuration.getInstance();
		String master = config.getProperty("master", "local");
		String appName = config.getProperty("app.name", "etl-visual");
		if(master.matches("^local(\\[\\w+\\])?$")) {
			System.setProperty("spark.master", master);
			System.setProperty("spark.app.name", appName);
			TaskRunner.main(new String[]{json});
		} else {
			String[] args = new String[] {
				"--master", master,
				"--name", appName,
				"--deploy-mode", config.getProperty("deploy.mode", "client"),
				"--class", TaskRunner.class.getName(),
				"file:///C:/Users/Ouyang/Desktop/spark-jars/etl-visual.jar",
				json
			};
			SparkSubmit.main(args);
		}
	}

}
