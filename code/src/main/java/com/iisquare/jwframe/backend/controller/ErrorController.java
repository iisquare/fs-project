package com.iisquare.jwframe.backend.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.CoreController;

@Controller
@Scope("prototype")
public class ErrorController extends CoreController {

	public Object indexAction (Exception e) throws Exception {
		e.printStackTrace(response.getWriter());
		return null;
	}
	
}
