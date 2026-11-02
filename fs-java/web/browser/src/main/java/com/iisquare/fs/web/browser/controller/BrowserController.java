package com.iisquare.fs.web.browser.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.browser.BrowserNodeService;
import com.iisquare.fs.web.browser.PlaywrightPool;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/browser")
public class BrowserController extends PermitControllerBase {

    @Autowired
    BrowserNodeService browserNodeService;

    @PostMapping("/fetch")
    public String fetchAction(@RequestBody ObjectNode request) {
        try {
            return ApiUtil.echoResult(0, null, browserNodeService.fetch(request));
        } catch (PlaywrightPool.BrowserBusyException e) {
            return ApiUtil.echoResult(503, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return ApiUtil.echoResult(400, e.getMessage(), null);
        } catch (Exception e) {
            return ApiUtil.echoResult(502, e.getMessage(), null);
        }
    }
}
