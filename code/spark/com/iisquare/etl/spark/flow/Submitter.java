package com.iisquare.etl.spark.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
			// 使用  --packages|--exclude-packages|--repositories处理依赖
			FlowService flowService = new FlowService();
			Set<String> jarsSet = flowService.generateJars(forceReload);
			List<String> argList = new ArrayList<>();
			argList.add("--master");
			argList.add(master);
			argList.add("--name");
			argList.add(appName);
			argList.add("--deploy-mode");
			argList.add(config.getProperty("deploy.mode", "client"));
			argList.add("--class");
			argList.add(TaskRunner.class.getName());
			if(!jarsSet.isEmpty()) {
				argList.add("--jars");
				argList.add(DPUtil.implode(",", DPUtil.collectionToArray(jarsSet)));
			}
			argList.add("build/libs/etl-visual.jar");
			argList.add(json);
			SparkSubmit.main(DPUtil.collectionToStringArray(argList));
		}
		return true;
	}

}
