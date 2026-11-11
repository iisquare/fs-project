package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.sitebackend.name}", url = "${rpc.sitebackend.rest}",
        fallbackFactory = SiteBackendFallback.class)
public interface SiteBackendRpc extends RpcBase {
}
