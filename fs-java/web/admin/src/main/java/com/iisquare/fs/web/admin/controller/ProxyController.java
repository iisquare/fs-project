package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.base.web.sse.SsePlainRequest;
import com.iisquare.fs.base.web.sse.SsePlainRequestPool;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import com.iisquare.fs.web.core.mvc.RpcBase;
import feign.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/proxy")
public class ProxyController extends ControllerBase implements InitializingBean, DisposableBean {

    @Autowired
    private WebApplicationContext context;

    private SsePlainRequestPool pool = new SsePlainRequestPool();

    @PostMapping("/post")
    public String postAction(@RequestBody Map<?, ?> param) {
        return request(RequestMethod.POST, param);
    }

    @PostMapping("/get")
    public String getAction(@RequestBody Map<?, ?> param) {
        return request(RequestMethod.GET, param);
    }

    @PostMapping("/postResponse")
    public String postResponseAction(@RequestBody Map<?, ?> param, HttpServletResponse response) throws Exception {
        return response(RequestMethod.POST, param, response);
    }

    @GetMapping("/getResponse")
    public String getResponseAction(@RequestParam Map<String, Object> param, HttpServletResponse response) throws Exception {
        String data = new String(Base64.decodeBase64(DPUtil.parseString(param.get("data"))));
        param.put("data", DPUtil.toJSON(DPUtil.parseJSON(data), Map.class));
        return response(RequestMethod.GET, param, response);
    }

    @PostMapping("/postSSE")
    public SseEmitter postSSEAction(HttpServletRequest request, @RequestBody Map<?, ?> param) throws Exception {
        return sse(request, RequestMethod.POST, param);
    }

    @GetMapping("/getSSE")
    public SseEmitter getSSEAction(HttpServletRequest request, @RequestParam Map<String, Object> param) throws Exception {
        String data = new String(Base64.decodeBase64(DPUtil.parseString(param.get("data"))));
        param.put("data", DPUtil.toJSON(DPUtil.parseJSON(data), Map.class));
        return sse(request, RequestMethod.GET, param);
    }

    @PostMapping("/upload")
    public String uploadAction(@RequestParam Map param, @RequestPart("file") MultipartFile file) {
        RpcBase rpc;
        try {
            rpc = rpc(DPUtil.parseString(param.get("app")));
        } catch (Exception e) {
            return ApiUtil.echoResult(4031, "应用不存在", e.getMessage());
        }
        String uri = DPUtil.parseString(param.get("uri"));
        String json = new String(Base64.decodeBase64(DPUtil.parseString(param.get("data"))));
        Map data = DPUtil.toJSON(DPUtil.parseJSON(json), Map.class);
        if (null == data) data = new LinkedHashMap();
        return rpc.upload(uri, file, data);
    }

    private String request(RequestMethod method, Map<?, ?> param) {
        RpcBase rpc;
        try {
            rpc = rpc(DPUtil.parseString(param.get("app")));
        } catch (Exception e) {
            return ApiUtil.echoResult(4031, "应用不存在", e.getMessage());
        }
        String uri = DPUtil.parseString(param.get("uri"));
        Map data = (Map) param.get("data");
        if (null == data) data = new HashMap();
        switch (method) {
            case POST: return rpc.post(uri, data);
            case GET: return rpc.get(uri, data);
            default: return ApiUtil.echoResult(4032, "请求方式不支持", null);
        }
    }

    private String response(RequestMethod method, Map<?, ?> param, HttpServletResponse response) throws Exception {
        RpcBase rpc;
        try {
            rpc = rpc(DPUtil.parseString(param.get("app")));
        } catch (Exception e) {
            return ApiUtil.echoResult(4031, "应用不存在", e.getMessage());
        }
        String uri = DPUtil.parseString(param.get("uri"));
        Map data = (Map) param.get("data");
        if (null == data) data = new HashMap();
        Response result;
        switch (method) {
            case POST: result = rpc.postResponse(uri, data); break;
            case GET: result = rpc.getResponse(uri, data); break;
            default: return ApiUtil.echoResult(4032, "请求方式不支持", null);
        }
        if (null == result) return ApiUtil.echoResult(500, "服务异常", null);
        if (result.status() == HttpStatus.OK.value()) {
            for (Map.Entry<String, Collection<String>> entry : result.headers().entrySet()) {
                for (String value : entry.getValue()) {
                    response.addHeader(entry.getKey(), value);
                }
            }
        }
        return IOUtils.toString(result.body().asInputStream());
    }

    private SseEmitter sse(HttpServletRequest r1, RequestMethod method, Map<?, ?> param) throws Exception {
        SsePlainEmitter emitter = new SsePlainEmitter(0L);
        String url = String.format("rpc.%s.rest", DPUtil.parseString(param.get("app"))).toLowerCase();
        url = context.getEnvironment().getProperty(url);
        if (DPUtil.empty(url)) {
            return emitter.error("app_not_found", "请求应用不存在", "admin", param, false).sync();
        }
        url += DPUtil.parseString(param.get("uri"));
        String finalUrl = url;
        SsePlainRequest request = new SsePlainRequest() { // 处理请求
            @Override
            public HttpRequestBase request() throws Exception {
                switch (method) {
                    case POST: {
                        HttpPost r2 = new HttpPost(finalUrl);
                        Charset charset = StandardCharsets.UTF_8;
                        r2.addHeader("Content-Type", "application/json;charset=" + charset.name());
                        String data = DPUtil.stringify(param.get("data"));
                        if (!DPUtil.empty(data)) {
                            r2.setEntity(new StringEntity(data, charset));
                        }
                        return pool.applyHeaders(r2, r1, FeignInterceptor.headers);
                    }
                    case GET: {
                        HttpGet r3 = new HttpGet(finalUrl);
                        return pool.applyHeaders(r3, r1, FeignInterceptor.headers);
                    }
                    default:
                        return null;
                }
            }

            @Override
            public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) { // 中断客户端处理请求
                emitter.error("backend_error", "服务端异常", "admin", throwable.getMessage(), isStream).abort();
            }

            @Override
            public boolean onMessage(CloseableHttpResponse response, String line, boolean isStream) {
                if ("".equals(line)) return emitter.isRunning();
                emitter.send(line, false);
                return emitter.isRunning();
            }
        };
        return pool.process(request, emitter);
    }

    public RpcBase rpc(String name) throws Exception {
        Class<?> rpc = Class.forName("com.iisquare.fs.web.core.rpc." + name + "Rpc");
        Object bean = context.getBean(rpc);
        if (!(bean instanceof RpcBase)) throw new RuntimeException("无效的RPC实例");
        return (RpcBase) bean;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public void destroy() throws Exception {
        FileUtil.close(pool);
    }
}
