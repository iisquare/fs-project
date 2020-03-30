package com.iisquare.fs.web.core.mvc;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

public interface RpcBase {

    @RequestMapping(method = RequestMethod.GET, value = "{uri}")
    String get(@PathVariable("uri") String uri, @RequestParam Map param);

    @RequestMapping(method = RequestMethod.POST, value = "{uri}")
    String post(@PathVariable("uri") String uri, @RequestBody Map param);

}
