package com.iisquare.fs.app.crawler.web.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.crawler.web.ControllerBase;
import com.iisquare.fs.app.crawler.web.Route;
import com.iisquare.fs.app.crawler.web.ControllerBase;
import com.iisquare.fs.app.crawler.web.Route;
import com.iisquare.fs.base.core.util.DPUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class IndexController extends ControllerBase {

    @Route("/")
    public String indexAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        ObjectNode info = DPUtil.objectNode();
        info.put("name", "crawler");
        info.put("version", "0.0.1");
        info.put("status", "running");
        return DPUtil.stringify(info);
    }

}
