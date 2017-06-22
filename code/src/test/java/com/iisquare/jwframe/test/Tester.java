package com.iisquare.jwframe.test;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

import com.iisquare.etl.spark.flow.Submitter;
import com.iisquare.jwframe.utils.FileUtil;

public class Tester {

	public static void main(String[] args) throws Exception {
		/*String json = FileUtil.getContent("src/main/webapp/WEB-INF/template/frontend/flow/test.json");
		boolean result = Submitter.submit(json, true);
		System.out.println(result);*/
		Project project = ProjectBuilder.builder().build();
	}

}
