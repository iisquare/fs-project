package com.iisquare.jwframe;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.iisquare.jwframe.routing.Router;

public class ApplicationServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private String appUri, rootPath;
	private Logger logger = Logger.getLogger(getClass().getName());

	@Override
	public void init() throws ServletException {
		appUri = getInitParameter("appUri");
		if (!appUri.startsWith("/")) appUri = "/" + appUri;
		if (!appUri.endsWith("/")) appUri += "/";
		rootPath = getServletContext().getRealPath("/");
		super.init();
		if(logger.isDebugEnabled()) logger.debug("ApplicationServlet.init");
	}

	@Override
	public void destroy() {
		super.destroy();
		if(logger.isDebugEnabled()) logger.debug("ApplicationServlet.destroy");
	}

	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Object result = new Router(appUri, rootPath, request, response).dispatch();
		if(null != result) throw new ServletException(result.toString());
	}

}
