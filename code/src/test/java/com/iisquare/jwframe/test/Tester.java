package com.iisquare.jwframe.test;

import java.util.List;
import com.iisquare.jwframe.utils.DPUtil;

public class Tester {

	public static void main(String[] args) throws Exception {
	    String sql = "SELECT * FROM t_demo WHERE id=:id and name=:name ORDER BY id LIMIT 1";
	    List<String> list = DPUtil.getMatcher(":[a-zA-Z0-9_]+", sql, false);
	    System.out.println(list);
	}

}
