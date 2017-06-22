package com.iisquare.etl.spark.flow;

import java.util.List;

import org.apache.spark.deploy.SparkSubmit;

import com.iisquare.etl.spark.config.Configuration;
import com.iisquare.jwframe.service.FlowService;
import com.iisquare.jwframe.utils.DPUtil;

public class Submitter {

	/**
	 * 参照：http://spark.apache.org/docs/latest/submitting-applications.html
	 */
	public static boolean submit(String json, boolean forceReload) throws Exception {
		Configuration config = Configuration.getInstance();
		String master = config.getProperty("master", "local");
		String appName = config.getProperty("app.name", "etl-visual");
		if(master.matches("^local(\\[\\w+\\])?$")) {
			System.setProperty("spark.master", master);
			System.setProperty("spark.app.name", appName);
			TaskRunner.main(new String[]{json});
		} else {
			FlowService flowService = new FlowService();
			List<String> jarList = flowService.generateJars(forceReload);
			if(jarList.isEmpty()) return false;
			String[] args = new String[] {
				"--master", master,
				"--name", appName,
				"--deploy-mode", config.getProperty("deploy.mode", "client"),
				"--class", TaskRunner.class.getName(),
				"--jars", DPUtil.implode(",", DPUtil.collectionToArray(jarList)),
				"build/libs/etl-visual.jar",
				json
			};
			System.out.println(DPUtil.implode(" ", args));
			SparkSubmit.main(args);
		}
		return true;
	}

}
