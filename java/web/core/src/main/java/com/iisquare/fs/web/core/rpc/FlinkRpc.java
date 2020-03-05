package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;

@Service
@FeignClient(name = "${rpc.flink.rest}", fallback = FlinkFallback.class)
public interface FlinkRpc extends RpcBase {

}
