package com.iisquare.fs.web.file.controller;

import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端直连服务
 */
@RestController
@RequestMapping("/file")
@RefreshScope
public class FileController extends PermitControllerBase {

}
