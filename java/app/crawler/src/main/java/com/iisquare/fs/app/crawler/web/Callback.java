package com.iisquare.fs.app.crawler.web;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Getter;

import java.lang.reflect.Method;

@Getter
public class Callback {

    private Object controller;
    private Method action;

    public Callback(Object controller, Method action) {
        this.controller = controller;
        this.action = action;
    }

    public String invoke(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        Object result = action.invoke(controller, context, request);
        return result == null ? null : result.toString();
    }

}
