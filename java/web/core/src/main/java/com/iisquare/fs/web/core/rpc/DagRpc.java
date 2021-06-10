package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.dag.name}", url = "${rpc.dag.rest}", fallbackFactory = DagFallback.class)
public interface DagRpc extends RpcBase {

}
