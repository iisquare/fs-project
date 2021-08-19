package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.CodeUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.core.mvc.RpcBase;
import feign.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/proxy")
public class ProxyController extends ControllerBase {

    @Autowired
    private WebApplicationContext context;

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

    public RpcBase rpc(String name) throws Exception {
        Class<?> rpc = Class.forName("com.iisquare.fs.web.core.rpc." + name + "Rpc");
        Object bean = context.getBean(rpc);
        if (!(bean instanceof RpcBase)) throw new RuntimeException("无效的RPC实例");
        return (RpcBase) bean;
    }

}
