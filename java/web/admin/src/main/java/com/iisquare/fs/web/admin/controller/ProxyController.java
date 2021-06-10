package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.core.mvc.RpcBase;
import com.iisquare.fs.web.core.rpc.*;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/proxy")
public class ProxyController extends ControllerBase {

    @Autowired
    private AnalyseRpc analyseRpc;
    @Autowired
    private DagRpc dagRpc;
    @Autowired
    private MemberRpc memberRpc;
    @Autowired
    private XlabRpc xlabRpc;
    @Autowired
    private SpiderRpc spiderRpc;
    @Autowired
    private FaceRpc faceRpc;
    @Autowired
    private LuceneRpc luceneRpc;
    @Autowired
    private FileRpc fileRpc;
    @Autowired
    private WorkerRpc workerRpc;
    @Autowired
    private BIRpc biRpc;
    @Autowired
    private OARpc oaRpc;
    @Autowired
    private CMSRpc cmsRpc;

    @PostMapping("/login")
    public String loginAction(@RequestBody Map<String, Object> param, HttpServletResponse response) throws IOException {
        param.put("module", "admin");
        Response result = memberRpc.login(param);
        if (null == result) return ApiUtil.echoResult(500, "服务异常", null);
        Map<String, Collection<String>> headers = result.headers();
        if (result.status() == HttpStatus.OK.value() && headers.containsKey(HttpHeaders.SET_COOKIE)) {
            for (String value : headers.get(HttpHeaders.SET_COOKIE)) {
                response.addHeader(HttpHeaders.SET_COOKIE, value);
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

    @PostMapping("/upload")
    public String uploadAction(@RequestParam String app, @RequestParam String uri, @RequestPart("file") MultipartFile file) {
        RpcBase rpc;
        switch (app) {
            case "dag": rpc = dagRpc; break;
            case "lucene": rpc = luceneRpc; break;
            case "file": rpc = fileRpc; break;
            default: return ApiUtil.echoResult(4031, "应用不存在", null);
        }
        return rpc.upload(uri, file);
    }

    private String request(RequestMethod method, Map<?, ?> param) {
        RpcBase rpc;
        switch (DPUtil.parseString(param.get("app"))) {
            case "analyse": rpc = analyseRpc; break;
            case "dag": rpc = dagRpc; break;
            case "member": rpc = memberRpc; break;
            case "xlab": rpc = xlabRpc; break;
            case "spider": rpc = spiderRpc; break;
            case "face": rpc = faceRpc; break;
            case "lucene": rpc = luceneRpc; break;
            case "file": rpc = fileRpc; break;
            case "worker": rpc = workerRpc; break;
            case "bi": rpc = biRpc; break;
            case "oa": rpc = oaRpc; break;
            case "cms": rpc = cmsRpc; break;
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
