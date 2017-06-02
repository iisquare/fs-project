package com.iisquare.jwframe.core;

import java.util.LinkedHashMap;
import java.util.Map;

import com.iisquare.jwframe.routing.Generator;
import com.iisquare.jwframe.routing.RouteAction;
import com.iisquare.jwframe.routing.Router;

public class Initializer {

	public Initializer() {
		init();
	}
	
	public void init() {
		Router.get("frontend", "/news/{date}/{id}.shtml", new Generator() {
			@Override
			public RouteAction call(String... args) {
				Map<String, String[]> params = new LinkedHashMap<>();
				params.put("date", new String[] {args[0]});
				params.put("id", new String[] {args[1]});
				return new RouteAction("index", "news", params);
			}
		});
	}
	
}
