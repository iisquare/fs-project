package com.iisquare.jwframe.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServletUtil;

public class Tester {

	public static void main(String[] args) throws Exception {
		Map<String, String[]> parameterMap = new LinkedHashMap<>();
		parameterMap.put("a", new String[]{"123", "1212"});
		parameterMap.put("b", new String[]{"456"});
		parameterMap.put("c[]", new String[]{"a", "b", "c"});
		parameterMap.put("d[0]", new String[]{"a"});
		parameterMap.put("d[2]", new String[]{"b"});
		parameterMap.put("d[3]", new String[]{"c"});
		Map<String, Object> params = ServletUtil.parseParameterMap(parameterMap);
		System.out.println(params);
		String regex = "^/login\\-(\\w+)\\-(\\w+)/$";
		String str = "/login-12-4/";
		List<String> list = DPUtil.getMatcher(regex, str, true, true);
		System.out.println(list);
	}

}
