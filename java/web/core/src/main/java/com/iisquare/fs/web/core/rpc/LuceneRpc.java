package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "${rpc.lucene.name}", url = "${rpc.lucene.rest}", fallbackFactory = LuceneFallback.class)
public interface LuceneRpc extends RpcBase {

    @RequestMapping(method = RequestMethod.GET, value = "/dictionary/plain")
    Response plain(@RequestParam Map param);

}
