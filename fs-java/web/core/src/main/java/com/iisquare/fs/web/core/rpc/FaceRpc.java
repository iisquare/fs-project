package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.face.name}", url = "${rpc.face.rest}", fallbackFactory = FaceFallback.class)
public interface FaceRpc extends RpcBase {

}
