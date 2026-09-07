package com.iisquare.fs.web.core.rpc;

import com.iisquare.fs.web.core.mvc.RpcBase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "${rpc.cron.name}", url = "${rpc.cron.rest}", fallbackFactory = CronFallback.class)
public interface CronRpc extends RpcBase {

    @PostMapping("/rpc/sync")
    String sync(@RequestBody Map<String, Object> param);

    @PostMapping("/rpc/trigger")
    String trigger(@RequestBody Map<String, Object> param);

    /**
     * 删除作业信息
     * {
     *     "jobs": [
     *         {
     *             "group": "分组1",
     *             "name": "作业1"
     *         },
     *         {
     *             "group": "分组2",
     *             "name": "作业2"
     *         }
     *     ]
     * }
     */
    @PostMapping("/rpc/delete")
    String delete(@RequestBody Map<String, Object> param);

}
