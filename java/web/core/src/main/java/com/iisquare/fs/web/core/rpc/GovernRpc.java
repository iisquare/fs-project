package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.govern.name}", url = "${rpc.govern.rest}", fallbackFactory = GovernFallback.class)
public interface GovernRpc extends RpcBase {

}
