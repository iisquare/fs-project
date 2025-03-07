package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.file.name}", url = "${rpc.file.rest}", fallbackFactory = FileFallback.class)
public interface FileRpc extends RpcBase {

}
