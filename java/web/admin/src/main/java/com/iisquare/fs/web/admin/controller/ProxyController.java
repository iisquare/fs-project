package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.mvc.RpcBase;
import com.iisquare.fs.web.core.rpc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
