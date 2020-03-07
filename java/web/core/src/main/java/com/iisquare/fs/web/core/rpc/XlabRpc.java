package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;

@FeignClient(name = "${rpc.xlab.name}", url = "${rpc.xlab.rest}", fallback = XlabFallback.class)
public interface XlabRpc extends RpcBase {

}
