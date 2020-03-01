package com.iisquare.fs.app.crawler.web;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;

public class ControllerBase {

    public String body(FullHttpRequest request) {
        ByteBuf buf = request.content();
        String content = buf.toString(io.netty.util.CharsetUtil.UTF_8);
        ReferenceCountUtil.release(buf);
        return content;
    }

}
