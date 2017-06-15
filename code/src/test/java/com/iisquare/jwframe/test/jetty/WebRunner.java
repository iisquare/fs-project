package com.iisquare.jwframe.test.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class WebRunner {

	public static void main(String[] args) {
		Server server = new Server(8080);
		try {
			WebAppContext context = new WebAppContext();
			context.setResourceBase("src/main/webapp");
			context.setContextPath("/etl-visual");
			context.setCompactPath(false);
			server.setHandler(context);
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
