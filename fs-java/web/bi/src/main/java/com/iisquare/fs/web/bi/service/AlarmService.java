package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.RestUtil;
import com.iisquare.fs.web.bi.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 作业运行预警服务。
 *
 * 预警信息始终记录到作业记录中，同时支持通过可配置的 Webhook 推送至外部告警通道（企业微信、钉钉、Slack等）。
 */
@Service
public class AlarmService {

    private static final Logger log = LoggerFactory.getLogger(AlarmService.class);

    @Value("${fs.bi.monitor.webhook:}")
    private String webhook;

    public ObjectNode alarm(Job job, String stage, String message) {
        ObjectNode json = DPUtil.objectNode();
        json.put("jobId", null == job.getId() ? 0 : job.getId());
        json.put("diagramId", null == job.getDiagramId() ? 0 : job.getDiagramId());
        json.put("name", DPUtil.parseString(job.getName()));
        json.put("engine", DPUtil.parseString(job.getEngine()));
        json.put("model", DPUtil.parseString(job.getModel()));
        json.put("remoteJobId", DPUtil.parseString(job.getJobId()));
        json.put("state", DPUtil.parseString(job.getState()));
        json.put("stage", stage);
        json.put("message", message);
        json.put("time", System.currentTimeMillis());
        log.warn("[bi-job-alarm] {}", DPUtil.stringify(json));
        if (!DPUtil.empty(webhook)) {
            RestUtil.Response response = RestUtil.postJson(webhook, DPUtil.stringify(json));
            json.put("webhookStatus", response.status());
        }
        return json;
    }

}
