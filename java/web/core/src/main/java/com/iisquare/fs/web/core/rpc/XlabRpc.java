package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.xlab.name}", url = "${rpc.xlab.rest}", fallbackFactory = XlabFallback.class)
public interface XlabRpc extends RpcBase {

}
