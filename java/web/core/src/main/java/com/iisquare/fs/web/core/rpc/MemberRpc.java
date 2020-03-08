package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@FeignClient(name = "${rpc.member.name}", url = "${rpc.member.rest}", fallbackFactory = MemberFallback.class)
public interface MemberRpc extends RpcBase {

    @RequestMapping(method = RequestMethod.POST, value = "/user/login")
    Response login(@RequestBody Map param);

}
