package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.spider.name}", url = "${rpc.spider.rest}", fallbackFactory = SpiderFallback.class)
public interface SpiderRpc extends RpcBase {

}
