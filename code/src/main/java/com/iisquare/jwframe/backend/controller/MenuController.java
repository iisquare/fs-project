package com.iisquare.jwframe.backend.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.RbacController;

@Controller
@Scope("prototype")
public class MenuController extends RbacController {

	public Object indexAction () throws Exception {
		return displayTemplate();
	}
	
}
