package com.iisquare.fs.web.demo.rpc;

import com.iisquare.fs.base.core.util.ApiUtil;
import org.springframework.stereotype.Component;

@Component
public class UserFallback implements UserRpc {
    @Override
    public String info(String tocken) {
        return ApiUtil.echoResult(500, "user info fallback", tocken);
    }
}
