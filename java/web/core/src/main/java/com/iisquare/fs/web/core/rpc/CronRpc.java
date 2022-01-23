package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rpc.cron.name}", url = "${rpc.cron.rest}", fallbackFactory = CronFallback.class)
public interface CronRpc extends RpcBase {

}
