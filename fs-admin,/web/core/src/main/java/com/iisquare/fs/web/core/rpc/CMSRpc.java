package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.cms.name}", url = "${rpc.cms.rest}", fallbackFactory = CMSFallback.class)
public interface CMSRpc extends RpcBase {

}
