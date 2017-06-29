package com.iisquare.jwframe.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.mvc.ServiceBase;

@Service
@Scope("singleton")
public class RoleService extends ServiceBase {
	
	@Autowired
	protected WebApplicationContext webApplicationContext;

}
