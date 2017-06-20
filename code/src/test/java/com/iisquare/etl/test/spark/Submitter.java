package com.iisquare.etl.test.spark;

import org.apache.spark.deploy.SparkSubmit;

import com.iisquare.etl.spark.config.Configuration;

public class Submitter {

	/**
	 * 参照：http://spark.apache.org/docs/latest/submitting-applications.html
	 */
	public static void main(String[] args) throws Exception {
		Configuration config = Configuration.getInstance();
		args = new String[] {
			"--master", config.getProperty("master", "local"),
			"--deploy-mode", config.getProperty("deploy.mode", "client"),
			"--name", config.getProperty("app.name", "etl-visual"),
			"--class", TaskRunner.class.getName(),
			"build/libs/code.jar"
		};
		SparkSubmit.main(args);
	}

}
