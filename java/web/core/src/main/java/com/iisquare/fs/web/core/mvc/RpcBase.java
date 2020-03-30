package com.iisquare.fs.web.core.mvc;

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
    String upload(@PathVariable("uri") String uri, @RequestPart(value = "file") MultipartFile file);

}
