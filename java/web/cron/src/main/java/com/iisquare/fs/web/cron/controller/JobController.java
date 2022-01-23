package com.iisquare.fs.web.cron.controller;

import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job")
public class JobController extends PermitControllerBase {
}
