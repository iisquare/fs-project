package com.iisquare.fs.web.core.mvc;

import feign.Param;
import feign.QueryMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

public interface RpcBase {

    @RequestMapping(method = RequestMethod.GET, value = "{uri}")
    String get(@Param("uri") String uri, @QueryMap Map param);

    @RequestMapping(method = RequestMethod.POST, value = "{uri}")
    String post(@Param("uri") String uri, @RequestBody Map param);

}
