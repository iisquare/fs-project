package com.iisquare.fs.web.lm.controller;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.MaintainControllerBase;
import com.iisquare.fs.web.lm.elasticsearch.KnowledgeChunkES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/maintain")
@RestController
public class MaintainController extends MaintainControllerBase {

    @Autowired
    KnowledgeChunkES chunkES;

    @GetMapping("/createChunk")
    public String createChunkAction(@RequestParam Map<String, Object> param) {
        boolean withAlias = !DPUtil.empty(param.get("withAlias"));
        return chunkES.create(withAlias);
    }

}
