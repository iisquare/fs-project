package com.iisquare.fs.web.demo.rpc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ne-user", url = "${rpc.ucenter.rest}", fallback = UserFallback.class)
public interface UserRpc {

    @RequestMapping(method = RequestMethod.GET, value = "/user/info")
    String info(@RequestParam("token") String token);

}
