package com.iisquare.jwframe.test.spark;

import com.iisquare.etl.spark.flow.Submitter;
import com.iisquare.jwframe.utils.FileUtil;

public class TestSubmitter {

	public static void main(String[] args) throws Exception {
		String json = FileUtil.getContent("src/main/webapp/WEB-INF/template/frontend/flow/test.json");
		boolean result = Submitter.submit(json, true, true);
		System.out.println(result);
	}

}
