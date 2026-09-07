package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.FallbackFactoryBase;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CronFallback extends FallbackFactoryBase<CronFallback> implements CronRpc {

    @Override
    public String sync(Map<String, Object> param) {
        return fallbackString();
    }

    @Override
    public String trigger(Map<String, Object> param) {
        return fallbackString();
    }

    @Override
    public String delete(Map<String, Object> param) {
        return fallbackString();
    }

}
