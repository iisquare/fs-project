package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.demo.name}", url = "${rpc.demo.rest}", fallbackFactory = DemoFallback.class)
public interface DemoRpc extends RpcBase {

}
