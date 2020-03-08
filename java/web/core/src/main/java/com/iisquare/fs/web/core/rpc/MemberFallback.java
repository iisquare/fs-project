package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.FallbackBase;
import feign.Response;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MemberFallback extends FallbackBase implements MemberRpc {

    @Override
    public Response login(Map param) {
        return null;
    }

}
