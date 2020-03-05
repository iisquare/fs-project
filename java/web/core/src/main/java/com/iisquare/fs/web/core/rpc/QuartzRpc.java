package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;

@Service
@FeignClient(name = "${rpc.quartz.rest}", fallback = QuartzFallback.class)
public interface QuartzRpc extends RpcBase {

}
