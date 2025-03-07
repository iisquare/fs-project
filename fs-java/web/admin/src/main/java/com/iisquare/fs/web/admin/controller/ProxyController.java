package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.sse.SsePlainEventBuilder;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.core.mvc.RpcBase;
import feign.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/proxy")
public class ProxyController extends ControllerBase implements InitializingBean, DisposableBean {

    @Autowired
    private WebApplicationContext context;

    private CloseableHttpClient client;
    private final ExecutorService pool = Executors.newCachedThreadPool();

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
    public SseEmitter postSSEAction(@RequestBody Map<?, ?> param) throws Exception {
        return sse(RequestMethod.POST, param);
    }

    @GetMapping("/getSSE")
    public SseEmitter getSSEAction(@RequestParam Map<String, Object> param) throws Exception {
        String data = new String(Base64.decodeBase64(DPUtil.parseString(param.get("data"))));
        param.put("data", DPUtil.toJSON(DPUtil.parseJSON(data), Map.class));
        return sse(RequestMethod.GET, param);
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

    private SseEmitter sse(RequestMethod method, Map<?, ?> param) throws Exception {
        String url = String.format("rpc.%s.rest", DPUtil.parseString(param.get("app"))).toLowerCase();
        url = context.getEnvironment().getProperty(url);
        if (DPUtil.empty(url)) return null;
        url += DPUtil.parseString(param.get("uri"));
        CloseableHttpResponse response;
        switch (method) {
            case POST: {
                HttpPost request = new HttpPost(url);
                String data = DPUtil.stringify(param.get("data"));
                if (!DPUtil.empty(data)) {
                    request.setEntity(new StringEntity(data));
                }
                response = this.client.execute(request);
                break;
            }
            case GET: {
                HttpGet request = new HttpGet(url);
                response = this.client.execute(request);
                break;
            }
            default:
                return null;
        }
        SseEmitter emitter = new SseEmitter();
        emitter.onCompletion(() -> {
            FileUtil.close(response);
        });
        emitter.onError(throwable -> {
            FileUtil.close(response);
        });
        pool.submit(() -> {
            InputStream stream = null;
            InputStreamReader reader = null;
            BufferedReader buffer = null;
            try {
                stream = response.getEntity().getContent();
                reader = new InputStreamReader(stream);
                buffer = new BufferedReader(reader);
                String line;
                while ((line = buffer.readLine()) != null) {
                    if (DPUtil.empty(line)) continue;
                    emitter.send(new SsePlainEventBuilder().line(line));
                }
            } catch (IOException ignored) {} finally {
                FileUtil.close(buffer, reader, stream);
                emitter.complete();
            }
        });
        return emitter;
    }

    public RpcBase rpc(String name) throws Exception {
        Class<?> rpc = Class.forName("com.iisquare.fs.web.core.rpc." + name + "Rpc");
        Object bean = context.getBean(rpc);
        if (!(bean instanceof RpcBase)) throw new RuntimeException("无效的RPC实例");
        return (RpcBase) bean;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.client = HttpClients.custom().build();
    }

    @Override
    public void destroy() throws Exception {
        pool.shutdown();
        client.close();
    }
}
