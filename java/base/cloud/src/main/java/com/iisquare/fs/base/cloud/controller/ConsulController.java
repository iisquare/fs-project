package com.iisquare.fs.base.cloud.controller;

import com.ecwid.consul.v1.ConsulClient;
import com.iisquare.fs.base.core.util.ApiUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/consul")
public class ConsulController {

    @Autowired
    private ConsulClient client;
    @Autowired(required = false)
    private Registration registration;

    @PutMapping("/deregister")
    public String deregisterAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        client.agentServiceDeregister(registration.getInstanceId());
        return ApiUtil.echoResult(0, null, registration);
    }

    @PutMapping("/register")
    public String registerAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        client.agentServiceRegister(((ConsulAutoRegistration) registration).getService());
        return ApiUtil.echoResult(0, null, registration);
    }

    @GetMapping("/registration")
    public String registrationAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return ApiUtil.echoResult(0, null, registration);
    }

}
