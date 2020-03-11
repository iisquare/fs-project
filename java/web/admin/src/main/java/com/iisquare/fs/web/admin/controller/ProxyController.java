package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.core.mvc.RpcBase;
import com.iisquare.fs.web.core.rpc.*;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    @Autowired
    private AnalyseRpc analyseRpc;
    @Autowired
    private FlinkRpc flinkRpc;
    @Autowired
    private MemberRpc memberRpc;
    @Autowired
    private QuartzRpc quartzRpc;
    @Autowired
    private XlabRpc xlabRpc;

    @PostMapping("/login")
    public String loginAction(@RequestBody Map<String, Object> param, HttpServletResponse response) throws IOException {
        param.put("module", "admin");
        Response result = memberRpc.login(param);
        Map<String, Collection<String>> headers = result.headers();
        if (result.status() == HttpStatus.OK.value() && headers.containsKey(HttpHeaders.SET_COOKIE)) {
            for (String value : headers.get(HttpHeaders.SET_COOKIE)) {
                List<HttpCookie> list = HttpCookie.parse(value);
                for (HttpCookie cookie : list) {
                    response.addCookie(ServletUtil.cookie(cookie));
                }
            }
        }
        return IOUtils.toString(result.body().asInputStream());
    }

    @PostMapping("/post")
    public String postAction(@RequestBody Map<?, ?> param) {
        return request(RequestMethod.POST, param);
    }

    @PostMapping("/get")
    public String getAction(@RequestBody Map<?, ?> param) {
        return request(RequestMethod.GET, param);
    }

    private String request(RequestMethod method, Map<?, ?> param) {
        RpcBase rpc;
        switch (DPUtil.parseString(param.get("app"))) {
            case "analyse": rpc = analyseRpc; break;
            case "flink": rpc = flinkRpc; break;
            case "member": rpc = memberRpc; break;
            case "quartz": rpc = quartzRpc; break;
            case "xlab": rpc = xlabRpc; break;
            default: return ApiUtil.echoResult(4031, "应用不存在", null);
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

}
