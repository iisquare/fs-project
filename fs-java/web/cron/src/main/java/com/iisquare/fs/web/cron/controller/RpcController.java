package com.iisquare.fs.web.cron.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.RpcControllerBase;
import com.iisquare.fs.web.cron.service.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/rpc")
public class RpcController extends RpcControllerBase {

    @Autowired
    RpcService rpcService;

    /**
     * 同步作业信息
     * {
     *     "group": "自定义分组名称，一般是业务类的类名",
     *     "name": "自定义作业名称，一般是业务主键",
     *     "expression": [ "0 0 1 * * ?" ], // 定时Cron表达式
     *     "app": "应用名称",
     *     "uri": "调用路径",
     *     "args": {} // 传递给被调用端的参数
     * }
     */
    @PostMapping("/sync")
    public String syncAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = rpcService.sync(param);
        return ApiUtil.echoResult(result);
    }

    /**
     * 手动触发作业
     * {
     *     "group": "自定义分组名称",
     *     "name": "自定义作业名称",
     *     "args": {} // 传递给被调用端的参数
     * }
     */
    @PostMapping("/trigger")
    public String triggerAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = rpcService.trigger(param);
        return ApiUtil.echoResult(result);
    }

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
    @PostMapping("/delete")
    public String deleteAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = rpcService.delete(param);
        return ApiUtil.echoResult(result);
    }

}
