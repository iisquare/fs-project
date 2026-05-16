package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.worker.name}", url = "${rpc.worker.rest}", fallbackFactory = WorkerFallback.class)
public interface WorkerRpc extends RpcBase {

}
