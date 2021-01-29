package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.spark.name}", url = "${rpc.spark.rest}", fallbackFactory = SparkFallback.class)
public interface SparkRpc extends RpcBase {

}
