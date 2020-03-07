package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;

@FeignClient(name = "${rpc.analyse.name}", url = "${rpc.analyse.rest}", fallback = AnalyseFallback.class)
public interface AnalyseRpc extends RpcBase {

}
