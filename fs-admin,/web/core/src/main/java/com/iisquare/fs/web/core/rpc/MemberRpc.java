package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.member.name}", url = "${rpc.member.rest}", fallbackFactory = MemberFallback.class)
public interface MemberRpc extends RpcBase {

}
