package com.iisquare.fs.web.lm.controller;

import com.iisquare.fs.base.web.mvc.ControllerBase;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class IndexController extends ControllerBase {

    @RequestMapping("/v1/chat/completions")
    public String completionAction() {
        return null;
    }

}
