package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.lucene.name}", url = "${rpc.lucene.rest}", fallbackFactory = LuceneFallback.class)
public interface LuceneRpc extends RpcBase {

}
