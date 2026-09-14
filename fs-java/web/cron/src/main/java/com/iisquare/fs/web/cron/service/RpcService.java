package com.iisquare.fs.web.cron.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import com.iisquare.fs.web.cron.dao.RpcLogDao;
import com.iisquare.fs.web.cron.entity.RpcLog;
import com.iisquare.fs.web.cron.job.RpcJob;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.quartz.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RpcService implements DisposableBean {

    @Autowired
    NodeService nodeService;
    @Autowired
    RpcLogDao rpcLogDao;
    @Autowired
    Environment environment;

    private final CloseableHttpClient client;

    public RpcService() {
        PoolingHttpClientConnectionManager pooling = new PoolingHttpClientConnectionManager();
        pooling.setMaxTotal(500);
        pooling.setDefaultMaxPerRoute(100);
        this.client = HttpClientBuilder.create().setConnectionManager(pooling).build();
    }

    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    public Map<String, Object> sync(Map<?, ?> param) {
        Scheduler scheduler = nodeService.scheduler();
        if (null == scheduler) return ApiUtil.result(1500, "调度服务未就绪", null);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "作业名称不能为空", name);
        String group = DPUtil.trim(DPUtil.parseString(param.get("group")));
        if (DPUtil.empty(group)) return ApiUtil.result(1002, "作业分组不能为空", group);
        String app = DPUtil.trim(DPUtil.parseString(param.get("app")));
        if (DPUtil.empty(app)) return ApiUtil.result(1003, "应用名称不能为空", app);
        String uri = DPUtil.trim(DPUtil.parseString(param.get("uri")));
        if (DPUtil.empty(uri)) return ApiUtil.result(1004, "调用路径不能为空", uri);

        JobDataMap data = new JobDataMap();
        for (Map.Entry<?, ?> entry : param.entrySet()) {
            data.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        JobDetail detail = JobBuilder.newJob(RpcJob.class)
                .withIdentity(JobKey.jobKey(name, group))
                .withDescription(String.format("RPC调用：%s%s", app, uri))
                .storeDurably()
                .usingJobData(data)
                .build();
        try {
            scheduler.addJob(detail, true);
        } catch (Exception e) {
            return ApiUtil.result(5001, "保存RPC作业失败", e.getMessage());
        }

        List<String> expressions = DPUtil.parseStringList(param.get("expression"));
        if (expressions.isEmpty()) {
            return ApiUtil.result(0, null, DPUtil.buildMap("jobKey", detail.getKey().toString()));
        }
        try {
            return syncTriggers(scheduler, detail.getKey(), expressions, data);
        } catch (Exception e) {
            return ApiUtil.result(5002, "同步RPC触发器失败", e.getMessage());
        }
    }

    private Map<String, Object> syncTriggers(Scheduler scheduler, JobKey jobKey, List<String> expressions, JobDataMap data) throws Exception {
        Set<TriggerKey> desired = new LinkedHashSet<>();
        for (int index = 0; index < expressions.size(); index++) {
            String expression = DPUtil.trim(expressions.get(index));
            if (DPUtil.empty(expression)) continue;
            TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName() + "@" + index, jobKey.getGroup());
            desired.add(triggerKey);
            TriggerBuilder<CronTrigger> builder = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobKey)
                    .withDescription(String.format("RPC调用表达式%d", index))
                    .usingJobData(data)
                    .withSchedule(CronScheduleBuilder.cronSchedule(expression)
                            .withMisfireHandlingInstructionDoNothing());
            Trigger oldTrigger = scheduler.getTrigger(triggerKey);
            if (null == oldTrigger) {
                scheduler.scheduleJob(builder.build());
            } else {
                scheduler.rescheduleJob(triggerKey, builder.build());
            }
        }
        // 覆盖式清理：当前作业下不在本次表达式列表中的触发器全部移除
        for (Trigger trigger : scheduler.getTriggersOfJob(jobKey)) {
            if (desired.contains(trigger.getKey())) continue;
            scheduler.unscheduleJob(trigger.getKey());
        }
        return ApiUtil.result(0, null, DPUtil.buildMap("jobKey", jobKey.toString(), "triggers", desired.size()));
    }

    public Map<String, Object> trigger(Map<?, ?> param) {
        Scheduler scheduler = nodeService.scheduler();
        if (null == scheduler) return ApiUtil.result(1500, "调度服务未就绪", null);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "作业名称不能为空", name);
        String group = DPUtil.trim(DPUtil.parseString(param.get("group")));
        if (DPUtil.empty(group)) return ApiUtil.result(1002, "作业分组不能为空", group);
        JobKey jobKey = JobKey.jobKey(name, group);
        try {
            JobDetail detail = scheduler.getJobDetail(jobKey);
            if (null == detail) return ApiUtil.result(1404, "作业信息不存在", jobKey.toString());
            JobDataMap data = null;
            if (param.containsKey("args")) { // 手动触发时可覆盖本次调用的参数
                data = new JobDataMap();
                data.put("args", param.get("args"));
            }
            scheduler.triggerJob(jobKey, data);
            return ApiUtil.result(0, null, DPUtil.buildMap("jobKey", jobKey.toString()));
        } catch (Exception e) {
            return ApiUtil.result(5003, "触发作业失败", e.getMessage());
        }
    }

    public Map<String, Object> delete(Map<?, ?> param) {
        Scheduler scheduler = nodeService.scheduler();
        if (null == scheduler) return ApiUtil.result(1500, "调度服务未就绪", null);
        Object value = param.get("jobs");
        if (!(value instanceof List<?>)) return ApiUtil.result(1001, "作业列表不能为空", value);
        List<String> failures = new ArrayList<>();
        int deleted = 0;
        for (Object item : (List<?>) value) {
            if (!(item instanceof Map<?, ?>)) continue;
            Map<?, ?> job = (Map<?, ?>) item;
            String name = DPUtil.trim(DPUtil.parseString(job.get("name")));
            String group = DPUtil.trim(DPUtil.parseString(job.get("group")));
            if (DPUtil.empty(name) || DPUtil.empty(group)) {
                failures.add(String.valueOf(item));
                continue;
            }
            JobKey jobKey = JobKey.jobKey(name, group);
            try {
                if (scheduler.deleteJob(jobKey)) {
                    deleted++;
                } else {
                    failures.add(jobKey.toString());
                }
            } catch (Exception e) {
                failures.add(jobKey + "：" + e.getMessage());
            }
        }
        return ApiUtil.result(0, null, DPUtil.buildMap("deleted", deleted, "failures", failures));
    }

    public Map<String, Object> invoke(Map<?, ?> param, String schedule, String jobName, String jobGroup,
                                      String triggerName, String triggerGroup) {
        long requestTime = System.currentTimeMillis();
        String app = DPUtil.trim(DPUtil.parseString(param.get("app")));
        String uri = DPUtil.trim(DPUtil.parseString(param.get("uri")));
        String requestBody = DPUtil.stringify(param.get("args"));
        RpcLog rpcLog = RpcLog.builder()
                .schedule(schedule)
                .jobName(jobName).jobGroup(jobGroup)
                .triggerName(triggerName).triggerGroup(triggerGroup)
                .app(app).uri(uri)
                .requestHeaders("")
                .requestBody(requestBody)
                .responseHeaders("")
                .status(0)
                .responseBody("")
                .state(RpcLog.State.RUNNING.name())
                .message("")
                .requestTime(requestTime)
                .responseTime(0L)
                .duration(0L)
                .build();
        rpcLog = rpcLogDao.save(rpcLog);
        long responseTime = requestTime;
        try {
            String url = resolveUrl(app, uri);
            HttpPost post = new HttpPost(url);
            post.setConfig(RequestConfig.custom()
                    .setConnectTimeout(30000)
                    .setSocketTimeout(30 * 60 * 1000)
                    .setConnectionRequestTimeout(30 * 60 * 1000)
                    .build());
            post.setHeader("Content-Type", "application/json;charset=UTF-8");
            Map<String, String> headers = FeignInterceptor.headers(null, environment);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                post.setHeader(entry.getKey(), entry.getValue());
            }
            if (!DPUtil.empty(requestBody)) {
                post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
            }
            rpcLog.setRequestHeaders(headers(post.getAllHeaders()));
            try (CloseableHttpResponse response = client.execute(post)) {
                responseTime = System.currentTimeMillis();
                int status = response.getStatusLine().getStatusCode();
                String body = null == response.getEntity() ? "" :
                        new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                rpcLog.setStatus(status);
                rpcLog.setResponseHeaders(headers(response.getAllHeaders()));
                rpcLog.setResponseBody(body);
                rpcLog.setState(status >= 200 && status < 300 ? RpcLog.State.SUCCEED.name() : RpcLog.State.FAILED.name());
                rpcLog.setMessage(response.getStatusLine().getReasonPhrase());
            }
        } catch (Exception e) {
            responseTime = System.currentTimeMillis();
            rpcLog.setState(RpcLog.State.FAILED.name());
            rpcLog.setMessage(e.getMessage());
        }
        rpcLog.setResponseTime(responseTime);
        rpcLog.setDuration(responseTime - requestTime);
        rpcLogDao.save(rpcLog);
        return RpcLog.State.SUCCEED.name().equals(rpcLog.getState())
                ? ApiUtil.result(0, null, rpcLog)
                : ApiUtil.result(5001, "RPC调用失败", rpcLog.getMessage());
    }

    private String resolveUrl(String app, String uri) {
        String rest = environment.getProperty(String.format("rpc.%s.rest", app).toLowerCase());
        if (DPUtil.empty(rest)) throw new RuntimeException("应用地址未配置：" + app);
        if (!rest.endsWith("/")) rest += "/";
        if (uri.startsWith("/")) uri = uri.substring(1);
        return rest + uri;
    }

    private String headers(Header[] headers) {
        Map<String, String> result = new LinkedHashMap<>();
        if (null == headers) return "{}";
        for (Header header : headers) {
            result.put(header.getName(), header.getValue());
        }
        return DPUtil.stringify(result);
    }

}
