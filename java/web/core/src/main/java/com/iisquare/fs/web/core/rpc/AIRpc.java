package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.ai.name}", url = "${rpc.ai.rest}", fallbackFactory = AIFallback.class)
public interface AIRpc extends RpcBase {

}
