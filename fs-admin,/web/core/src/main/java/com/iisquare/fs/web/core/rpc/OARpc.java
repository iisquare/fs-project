package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.oa.name}", url = "${rpc.oa.rest}", fallbackFactory = OAFallback.class)
public interface OARpc extends RpcBase {

}
