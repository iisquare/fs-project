package com.iisquare.fs.web.core.mvc;

import feign.Response;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface RpcBase {

    @RequestMapping(method = RequestMethod.GET, value = "{uri}")
    String get(@PathVariable("uri") String uri, @RequestParam Map param);

    @RequestMapping(method = RequestMethod.POST, value = "{uri}")
    String post(@PathVariable("uri") String uri, @RequestBody Map param);

    @RequestMapping(value = "{uri}", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String upload(@PathVariable("uri") String uri, @RequestPart(value = "file") MultipartFile file, @RequestParam Map param);

    @RequestMapping(method = RequestMethod.GET, value = "{uri}")
    Response getResponse(@PathVariable("uri") String uri, @RequestParam Map param);

    @RequestMapping(method = RequestMethod.POST, value = "{uri}")
    Response postResponse(@PathVariable("uri") String uri, @RequestBody Map param);

}
