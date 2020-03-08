package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.quartz.name}", url = "${rpc.quartz.rest}", fallbackFactory = QuartzFallback.class)
public interface QuartzRpc extends RpcBase {

}
