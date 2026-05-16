package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.bi.name}", url = "${rpc.bi.rest}", fallbackFactory = BIFallback.class)
public interface BIRpc extends RpcBase {

}
