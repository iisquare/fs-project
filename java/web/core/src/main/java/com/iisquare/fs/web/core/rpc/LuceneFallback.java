package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.FallbackFactoryBase;
import feign.Response;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LuceneFallback extends FallbackFactoryBase implements LuceneRpc {

    @Override
    public Response plain(Map param) {
        return null;
    }

}
