package com.iisquare.fs.web.quartz.controller;

import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.quartz.entity.RunLog;
import com.iisquare.fs.web.quartz.service.JobService;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.quartz.CronExpression;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value="/job")
@RefreshScope
@Api(description = "作业管理")
@SuppressWarnings("unchecked")
public class JobController extends ControllerBase {

    @Autowired
    private JobService jobService;
    @Value("${spring.application.name}")
    private String appName;

    @PostMapping("/list")
    @ApiOperation(value = "获取作业列表")
    public String listAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>name</td><td>string</td><td>N</td><td>无</td><td>作业名称</td></tr>" +
            "<tr><td>group</td><td>string</td><td>N</td><td>无</td><td>作业分组</td></tr>" +
            "<tr><td>page</td><td>int</td><td>N</td><td>1</td><td>当前页码</td></tr>" +
            "<tr><td>pageSize</td><td>int</td><td>N</td><td>15</td><td>分页大小，最大为100</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        return ApiUtil.echoResult(0, null, jobService.list(params));
    }

    @PostMapping("/map")
    @ApiOperation(value = "根据作业唯一标识数组获取作业信息")
    public String mapAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>keys</td><td>array</td><td>Y</td><td>无</td><td>作业唯一标识数组</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        List<JobKey> keys = getJobKeys(params.get("keys"));
        if (keys.isEmpty()) {
            return ApiUtil.echoResult(1001, "参数错误", null);
        }
        return ApiUtil.echoResult(0, null, jobService.map(keys));
    }

    @PostMapping("/add")
    @ApiOperation(value = "添加作业")
    public String addAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>name</td><td>string</td><td>Y</td><td>无</td><td>作业名称</td></tr>" +
            "<tr><td>group</td><td>string</td><td>Y</td><td>无</td><td>作业分组</td></tr>" +
            "<tr><td>cron</td><td>string</td><td>N</td><td>无</td><td>quartz时间表达式</td></tr>" +
            "<tr><td>jobClass</td><td>string</td><td>Y</td><td>无</td><td>作业类型</td></tr>" +
            "<tr><td>params</td><td>object</td><td>N</td><td>无</td><td>参数</td></tr>" +
            "<tr><td>desc</td><td>string</td><td>N</td><td>无</td><td>描述</td></tr>" +
            "<tr><td>uid</td><td>int</td><td>N</td><td>无</td><td>操作者</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        String name = DPUtil.trim(DPUtil.parseString(params.get("name")));
        String group = DPUtil.trim(DPUtil.parseString(params.get("group")));
        String cron = DPUtil.trim(DPUtil.parseString(params.get("cron")));
        String jobClass = DPUtil.trim(DPUtil.parseString(params.get("jobClass")));
        if (DPUtil.empty(name) || DPUtil.empty(group) || !cron.isEmpty() && !CronExpression.isValidExpression(cron) || DPUtil.empty(jobClass)) {
            return ApiUtil.echoResult(1001, "参数错误", null);
        }
        Object paramsObj = params.get("params");
        Map<String, Object> extParams = paramsObj instanceof Map ? (Map<String, Object>) paramsObj : new LinkedHashMap<>();
        String desc = DPUtil.trim(DPUtil.parseString(params.get("desc")));
        int uid = DPUtil.parseInt(params.get("uid"));
        Map<String, Object> result = jobService.add(name, group, cron, jobClass, extParams, desc, uid, false);
        return ApiUtil.echoResult(DPUtil.parseInt(result.get("code")), DPUtil.parseString(result.get("message")), null);
    }

    @PostMapping("/modify")
    @ApiOperation(value = "修改作业")
    public String modifyAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>name</td><td>string</td><td>Y</td><td>无</td><td>作业名称</td></tr>" +
            "<tr><td>group</td><td>string</td><td>Y</td><td>无</td><td>作业分组</td></tr>" +
            "<tr><td>cron</td><td>string</td><td>N</td><td>无</td><td>quartz时间表达式</td></tr>" +
            "<tr><td>jobClass</td><td>string</td><td>Y</td><td>无</td><td>作业类型</td></tr>" +
            "<tr><td>params</td><td>object</td><td>N</td><td>无</td><td>参数</td></tr>" +
            "<tr><td>desc</td><td>string</td><td>N</td><td>无</td><td>描述</td></tr>" +
            "<tr><td>uid</td><td>int</td><td>N</td><td>无</td><td>操作者</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        String name = DPUtil.trim(DPUtil.parseString(params.get("name")));
        String group = DPUtil.trim(DPUtil.parseString(params.get("group")));
        String cron = DPUtil.trim(DPUtil.parseString(params.get("cron")));
        String jobClass = DPUtil.trim(DPUtil.parseString(params.get("jobClass")));
        if (DPUtil.empty(name) || DPUtil.empty(group) || !cron.isEmpty() && !CronExpression.isValidExpression(cron) || DPUtil.empty(jobClass)) {
            return ApiUtil.echoResult(1001, "参数错误", null);
        }
        Object paramsObj = params.get("params");
        Map<String, Object> extParams = paramsObj instanceof Map ? (Map<String, Object>) paramsObj : new LinkedHashMap<>();
        String desc = DPUtil.trim(DPUtil.parseString(params.get("desc")));
        int uid = DPUtil.parseInt(params.get("uid"));
        Map<String, Object> result = jobService.add(name, group, cron, jobClass, extParams, desc, uid, true);
        return ApiUtil.echoResult(DPUtil.parseInt(result.get("code")), DPUtil.parseString(result.get("message")), null);
    }

    @PostMapping("/pause")
    @ApiOperation(value = "暂停作业")
    public String pauseAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>keys</td><td>array</td><td>Y</td><td>无</td><td>作业唯一标识数组</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        List<JobKey> keys = getJobKeys(params.get("keys"));
        if (keys.isEmpty()) {
            return ApiUtil.echoResult(1001, "参数错误", null);
        }
        int uid = DPUtil.parseInt(params.get("uid"));
        List<JobKey> result = jobService.pause(keys, uid);
        return ApiUtil.echoResult(result.isEmpty() ? 500 : 0, null, result);
    }

    @PostMapping("/resume")
    @ApiOperation(value = "恢复作业")
    public String resumeAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>keys</td><td>array</td><td>Y</td><td>无</td><td>作业唯一标识数组</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        List<JobKey> keys = getJobKeys(params.get("keys"));
        if (keys.isEmpty()) {
            return ApiUtil.echoResult(1001, "参数错误", null);
        }
        int uid = DPUtil.parseInt(params.get("uid"));
        List<JobKey> result = jobService.resume(keys, uid);
        return ApiUtil.echoResult(result.isEmpty() ? 500 : 0, null, result);
    }

    @PostMapping("/run")
    @ApiOperation(value = "执行作业")
    public String runAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>name</td><td>string</td><td>Y</td><td>无</td><td>作业名称</td></tr>" +
            "<tr><td>group</td><td>string</td><td>Y</td><td>无</td><td>作业分组</td></tr>" +
            "<tr><td>params</td><td>object</td><td>N</td><td>无</td><td>参数</td></tr>" +
            "<tr><td>uid</td><td>int</td><td>N</td><td>无</td><td>操作者</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        String name = DPUtil.trim(DPUtil.parseString(params.get("name")));
        String group = DPUtil.trim(DPUtil.parseString(params.get("group")));
        if (DPUtil.empty(name) || DPUtil.empty(group)) {
            return ApiUtil.echoResult(1001, "参数错误", null);
        }
        Object paramsObj = params.get("params");
        Map<String, Object> extParams = paramsObj instanceof Map ? (Map<String, Object>) paramsObj : new LinkedHashMap<>();
        int uid = DPUtil.parseInt(params.get("uid"));
        Map<String, Object> result = jobService.run(name, group, extParams, uid);
        return ApiUtil.echoResult(DPUtil.parseInt(result.get("code")), DPUtil.parseString(result.get("message")), null);
    }

    @PostMapping("/delete")
    @ApiOperation(value = "删除作业")
    public String deleteAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>keys</td><td>array</td><td>Y</td><td>无</td><td>作业唯一标识数组</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        List<JobKey> keys = getJobKeys(params.get("keys"));
        if (keys.isEmpty()) {
            return ApiUtil.echoResult(1001, "参数错误", null);
        }
        int uid = DPUtil.parseInt(params.get("uid"));
        List<JobKey> result = jobService.delete(keys, uid);
        return ApiUtil.echoResult(result.isEmpty() ? 500 : 0, null, result);
    }

    @PostMapping("/logs")
    @ApiOperation(value = "获取日志列表")
    public String logsAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>type</td><td>string</td><td>Y</td><td>无</td><td>日志类型（op-操作日志，run-运行日志）</td></tr>" +
            "<tr><td>name</td><td>string</td><td>N</td><td>无</td><td>作业名称</td></tr>" +
            "<tr><td>group</td><td>string</td><td>N</td><td>无</td><td>作业分组</td></tr>" +
            "<tr><td>page</td><td>int</td><td>N</td><td>1</td><td>当前页码</td></tr>" +
            "<tr><td>pageSize</td><td>int</td><td>N</td><td>15</td><td>分页大小，最大为100</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        return ApiUtil.echoResult(0, null, jobService.logs(params));
    }

    @PostMapping("/log")
    @ApiOperation(value = "获取日志详情")
    public String logAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>tag</td><td>string</td><td>Y</td><td>无</td><td>日志标签</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        String tag = DPUtil.parseString(params.get("tag"));
        if (tag.isEmpty()) {
            return ApiUtil.echoResult(1001, "参数错误", null);
        }
        RunLog result = jobService.log(tag);
        return ApiUtil.echoResult(result == null ? 404 : 0, null, result);
    }

    @PostMapping("/config")
    @ApiOperation(value = "获取配置")
    public String configAction(ModelMap model) {
        model.put("jobClass", jobService.jobClassConfig());
        model.put("status", jobService.statusConfig());
        model.put("runLogType", jobService.runLogTypeConfig());
        model.put("runLogResult", jobService.runLogResultConfig());
        model.put("opLogType", jobService.opLogTypeConfig());
        return ApiUtil.echoResult(0, null, model);
    }

    @PostMapping("/start")
    @ApiOperation(value = "启动调度")
    public String startAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>url</td><td>string</td><td>N</td><td>无</td><td>请求地址</td></tr>" +
            "<tr><td>uid</td><td>int</td><td>N</td><td>无</td><td>操作者</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params, HttpServletRequest request) {
        String url = DPUtil.trim(DPUtil.parseString(params.get("url")));
        String host = getServerHost(request);
        if (!url.isEmpty() && !url.contains(host)) {
            return getRemoteResult(DPUtil.trimRight(url, "/") + request.getRequestURI(), params);
        }
        int uid = DPUtil.parseInt(params.get("uid"));
        boolean result = jobService.start(host, uid);
        return ApiUtil.echoResult(result ? 0 : 500, null, null);
    }

    @PostMapping("/standby")
    @ApiOperation(value = "暂停调度")
    public String standbyAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>url</td><td>string</td><td>N</td><td>无</td><td>请求地址</td></tr>" +
            "<tr><td>uid</td><td>int</td><td>N</td><td>无</td><td>操作者</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params, HttpServletRequest request) {
        String url = DPUtil.trim(DPUtil.parseString(params.get("url")));
        String host = getServerHost(request);
        if (!url.isEmpty() && !url.contains(host)) {
            return getRemoteResult(DPUtil.trimRight(url, "/") + request.getRequestURI(), params);
        }
        int uid = DPUtil.parseInt(params.get("uid"));
        boolean result = jobService.standby(host, uid);
        return ApiUtil.echoResult(result ? 0 : 500, null, null);
    }

    @PostMapping("/isstandby")
    @ApiOperation(value = "判断节点是否暂停调度")
    public String isStandbyAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>url</td><td>string</td><td>N</td><td>无</td><td>请求地址</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params, HttpServletRequest request) {
        String url = DPUtil.trim(DPUtil.parseString(params.get("url")));
        if (!url.isEmpty() && !url.contains(getServerHost(request))) {
            return getRemoteResult(DPUtil.trimRight(url, "/") + request.getRequestURI(), params);
        }
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("isStandby", jobService.isStandby());
        return ApiUtil.echoResult(0, null, result);
    }

    @PostMapping("/isvalidcron")
    @ApiOperation(value = "判断quartz时间表达式是否合法")
    public String isValidCronAction(@ApiParam(name = "params", value = "<table border=\"1\" style=\"font-size:12px;text-indent:10px;\">" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>cron</td><td>string</td><td>Y</td><td>无</td><td>quartz时间表达式</td></tr>" +
            "</table>") @RequestBody Map<String, Object> params) {
        String cron = DPUtil.trim(DPUtil.parseString(params.get("cron")));
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("isValidCron", cron.isEmpty() || CronExpression.isValidExpression(cron));
        return ApiUtil.echoResult(0, null, result);
    }

    private List<JobKey> getJobKeys(Object params) {
        List<JobKey> jobKeys = new ArrayList<>();
        if (params instanceof List) {
            for (Object item : (List) params) {
                if (item instanceof LinkedHashMap) {
                    Map<String, String> map = (LinkedHashMap) item;
                    if (map.containsKey("name") && map.containsKey("group")) {
                        jobKeys.add(JobKey.jobKey(map.get("name"), map.get("group")));
                    }
                }
            }
        }
        return jobKeys;
    }

    private String getServerHost(HttpServletRequest request) {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ':' + request.getServerPort();
        } catch (UnknownHostException e) {
            return "UnknownHost";
        }
    }

    private String getRemoteResult(String url, Map<String, Object> params) {
        params.remove("url");
        RestTemplate restTemplate = new RestTemplate();
        try {
            return restTemplate.postForObject(url, params, String.class);
        } catch (Exception e) {
            return ApiUtil.echoResult(500, "转发失败", null);
        }
    }
}
