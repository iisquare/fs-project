package com.iisquare.jwframe.backend.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.CoreController;

@Controller
@Scope("prototype")
public class IndexController extends CoreController {

	public Object indexAction () throws Exception {
		return displayTemplate();
	}
	
}
