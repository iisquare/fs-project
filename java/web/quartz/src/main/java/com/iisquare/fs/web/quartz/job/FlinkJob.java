package com.iisquare.fs.web.quartz.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.quartz.service.FlinkService;
import com.iisquare.fs.web.quartz.service.JobService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class FlinkJob extends QuartzJobBean {

    @Autowired
    private FlinkService flinkService;
    @Autowired
    private JobService jobService;

    protected void executeInternal(@NonNull JobExecutionContext context) {
        JobDetail jobDetail = context.getJobDetail();
        JobKey jobKey = jobDetail.getKey();
        String logId = jobKey.getGroup() + "-" + jobKey.getName();
        int result = JobService.RUN_LOG_RESULT_SUBMIT_FAILED;
        ObjectNode oriResult = null;
        int code = 500;
        String message;
        try {
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            JsonNode flow = DPUtil.parseJSON(DPUtil.parseString(flinkService.plain(jobDataMap)));
            if (flow != null) {
                jobDataMap.put("logId", logId);
                oriResult = (ObjectNode) DPUtil.parseJSON(flinkService.submit(jobDataMap));
                if (oriResult != null && oriResult.has("code")) {
                    code = oriResult.get("code").asInt();
                    message = oriResult.findPath("message").asText();
                    if (code == 0) {
                        String pre = "Job has been submitted with JobID ";
                        String jobId = DPUtil.getFirstMatcher(pre + "[0-9a-f]{32}", message);
                        if (jobId == null) {
                            code = 500;
                        } else {
                            jobDataMap.put("jobId", jobId.replace(pre, ""));
                            result = JobService.RUN_LOG_RESULT_RUNNING;
                        }
                    }
                } else {
                    message = "提交失败";
                }
            } else {
                message = "获取流程图信息失败";
            }
        } catch (Exception e) {
            message = e.getMessage();
        }
        if (oriResult == null) {
            oriResult = DPUtil.objectNode();
        }
        oriResult.put("code", code);
        oriResult.put("message", message);
        jobService.addRunLog(context, result, DPUtil.stringify(oriResult));
    }
}
