package com.iisquare.jwframe.backend.controller;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.CoreController;
import com.iisquare.jwframe.routing.Router;
import com.iisquare.jwframe.service.UserService;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServletUtil;

@Controller
@Scope("prototype")
public class ErrorController extends CoreController {

	@Autowired
	protected UserService userService;
	
	public Object indexAction (Exception e) throws Exception {
		boolean isAjax = ServletUtil.isAjax(request);
		Throwable cause = e.getCause();
		if(null != cause &&"403".equals(cause.getMessage())) {
			if(DPUtil.empty(userService.getCurrentUserInfo(request))) {
				if(isAjax) {
					return displayMessage(403, "登录超时，请登陆后再试！", appPath + "login/?forward=back");
				} else {
					return redirect(appPath + "login/?forward=back");
				}
			}
			return isAjax ? displayMessage(403, "权限不足，禁止访问", cause.getMessage()) : displayTemplate("403");
		}
		if(DPUtil.isItemExist(new String[]{
			Router.ExceptionMessage.APP_URI_ERROR,
			Router.ExceptionMessage.NO_MODULE_MATCHES,
			Router.ExceptionMessage.NO_ROUTE_FOUND,
			Router.ExceptionMessage.NO_ROUTE_MATCHES
		}, e.getMessage())) return isAjax ? displayMessage(404, "页面不存在", e.getMessage()) : displayTemplate("404");
		StringWriter writer = new StringWriter();
		PrintWriter print = new PrintWriter(writer, true);
		e.printStackTrace(print);
		String trace = writer.getBuffer().toString();
		print.close();
		writer.close();
		assign("trace", trace);
		return isAjax ? displayMessage(500, "系统异常", trace) : displayTemplate("500");
	}
	
}
