package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.agent.name}", url = "${rpc.agent.rest}", fallbackFactory = AgentFallback.class)
public interface AgentRpc extends RpcBase {

}
