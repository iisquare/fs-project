package com.iisquare.etl.test.spark;

import com.iisquare.etl.spark.flow.Node;

public class ReadMySQLNode extends Node {

	@Override
	public Object process() {
		System.out.println(this.getClass());
		return null;
	}

}
