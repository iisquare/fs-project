package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.kg.name}", url = "${rpc.kg.rest}", fallbackFactory = KGFallback.class)
public interface KGRpc extends RpcBase {

}
