package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;

@FeignClient(name = "${rpc.flink.name}", url = "${rpc.flink.rest}", fallbackFactory = FlinkFallback.class)
public interface FlinkRpc extends RpcBase {

}
