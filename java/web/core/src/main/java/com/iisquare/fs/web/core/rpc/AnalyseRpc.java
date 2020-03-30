package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.analyse.name}", url = "${rpc.analyse.rest}", fallbackFactory = AnalyseFallback.class)
public interface AnalyseRpc extends RpcBase {

}
