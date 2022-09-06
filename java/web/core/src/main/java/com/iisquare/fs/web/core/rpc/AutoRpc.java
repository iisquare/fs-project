package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.auto.name}", url = "${rpc.auto.rest}", fallbackFactory = AutoFallback.class)
public interface AutoRpc extends RpcBase {

}
