package com.iisquare.fs.app.crawler.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpHandler extends ChannelInboundHandlerAdapter {

    protected final static Logger logger = LoggerFactory.getLogger(HttpHandler.class);
    private static Map<Channel, WebSocketServerHandshaker> handshakers = new ConcurrentHashMap<>();
    private static Map<String, Callback> routes = new LinkedHashMap<>();
    private static final String CONTROLLER_PACKAGE = "controller";
    private static final String CONTROLLER_SUFFIX = "Controller";
    private static final String ACTION_SUFFIX = "Action";

    static {
        String classpath = HttpHandler.class.getName();
        classpath = classpath.substring(0, classpath.length() - HttpHandler.class.getSimpleName().length());
        List<String> list = ReflectUtil.getClassName(classpath + CONTROLLER_PACKAGE);
        for (String item : list) {
            if (!item.endsWith(CONTROLLER_SUFFIX)) continue;
            try {
                Class<?> controller = Class.forName(item);
                Object instance = controller.newInstance();
                String controllerName = controller.getSimpleName();
                controllerName = controllerName.substring(0, controllerName.length() - CONTROLLER_SUFFIX.length());
                controllerName = DPUtil.lowerCaseFirst(controllerName);
                for (Method method : controller.getMethods()) {
                    String actionName = method.getName();
                    if (!actionName.endsWith(ACTION_SUFFIX)) continue;
                    actionName = actionName.substring(0, actionName.length() - ACTION_SUFFIX.length());
                    Route route = method.getAnnotation(Route.class);
                    String key = null == route ? "/" + controllerName + "/" + actionName + "/" : route.value();
                    routes.put(key, new Callback(instance, method));
                }
            } catch (Exception e) {
                logger.warn("load controller failed from " + item, e);
                continue;
            }
        }
    }

    public static JsonNode routes() {
        ObjectNode data = DPUtil.objectNode();
        for (Map.Entry<String, Callback> entry : routes.entrySet()) {
            data.put(entry.getKey(), entry.getValue().getAction().toString());
        }
        return data;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    public void write(ChannelHandlerContext ctx, FullHttpRequest req, String content) throws UnsupportedEncodingException {
        if (null == content) content = "";
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes("UTF-8")));
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Origin, X-Requested-With, Content-Type, Accept");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT,DELETE");
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH,response.content().readableBytes());
        sendHttpResponse(ctx, req, response);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        if (req.method().equals(HttpMethod.OPTIONS)) {
            write(ctx, req, null);
            return;
        }
        String uri = req.uri();
        if (!"websocket".equals(req.headers().get("Upgrade"))) {
            Callback callback = routes.get(uri);
            if (null == callback) {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
            } else {
                try {
                    write(ctx, req, callback.invoke(ctx, req));
                } catch (Exception e) {
                    logger.warn("dispatch " + uri + " error", e);
                    sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR));
                }
            }
            return;
        }
        if (!SocketLogger.LOGGER_PATH.equals(uri)) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        // 构造握手响应返回，本机测试
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(SocketLogger.LOGGER_PATH, null, false);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            handshakers.put(ctx.channel(), handshaker);
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) { // 判断是否是关闭链路的指令
            handshakers.remove(ctx.channel()).close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
        } else if (frame instanceof PingWebSocketFrame) { // 判断是否是Ping消息
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        } else {
            String message = (frame instanceof TextWebSocketFrame) ? ((TextWebSocketFrame) frame).text() : null;
            for (Map.Entry<Channel, WebSocketServerHandshaker> entry : handshakers.entrySet()) {
                entry.getKey().write(new TextWebSocketFrame(message));
            }
        }
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            try {
                res.content().writeBytes(buf);
            } finally {
                ReferenceCountUtil.release(buf);
            }
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }
        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

}
