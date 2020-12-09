package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.admin.mvc.AdminControllerBase;
import com.iisquare.fs.web.core.rpc.LuceneRpc;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Map;

@Controller
@RequestMapping("/lucene")
public class LuceneController extends AdminControllerBase {

    @Autowired
    private LuceneRpc luceneRpc;

    @GetMapping("/plain")
    @ResponseBody
    public String plainAction(@RequestParam Map param, HttpServletResponse response) throws Exception {
        Response result = luceneRpc.plain(param);
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

}
