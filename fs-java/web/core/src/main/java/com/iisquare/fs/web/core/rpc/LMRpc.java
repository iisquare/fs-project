package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.lm.name}", url = "${rpc.lm.rest}", fallbackFactory = LMFallback.class)
public interface LMRpc extends RpcBase {

}
