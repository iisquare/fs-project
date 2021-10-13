package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.shop.name}", url = "${rpc.shop.rest}", fallbackFactory = ShopFallback.class)
public interface ShopRpc extends RpcBase {

}
