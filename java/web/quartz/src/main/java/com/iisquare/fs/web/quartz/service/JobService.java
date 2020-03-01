package com.iisquare.fs.web.quartz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.web.quartz.dao.JobDao;
import com.iisquare.fs.web.quartz.dao.OpLogDao;
import com.iisquare.fs.web.quartz.dao.RunLogDao;
import com.iisquare.fs.web.quartz.entity.OpLog;
import com.iisquare.fs.web.quartz.entity.RunLog;
import com.iisquare.fs.web.quartz.job.FlinkJob;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.persistence.criteria.Predicate;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RefreshScope
@SuppressWarnings("unchecked")
public class JobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    //作业类型
    private static final String JOB_CLASS_FLINK = "com.iisquare.fs.web.quartz.job.FlinkJob";

    //运行日志类型
    private static final int RUN_LOG_TYPE_NORMAL = 1;
    private static final int RUN_LOG_TYPE_MANUAL = 2;

    //运行日志结果
    public static final int RUN_LOG_RESULT_SUBMIT_FAILED = 1;
    public static final int RUN_LOG_RESULT_RUNNING = 2;
    public static final int RUN_LOG_RESULT_FINISHED = 3;
    public static final int RUN_LOG_RESULT_FAILED = 4;
    public static final int RUN_LOG_RESULT_CANCELED = 5;

    //手动运行后缀
    private static final String RUN_MANUAL_SUFFIX = "#MANUAL#";

    //操作日志类型
    private static final int OP_LOG_TYPE_ADD = 1;
    private static final int OP_LOG_TYPE_MODIFY = 2;
    private static final int OP_LOG_TYPE_PAUSE = 3;
    private static final int OP_LOG_TYPE_RESUME = 4;
    private static final int OP_LOG_TYPE_RUN = 5;
    private static final int OP_LOG_TYPE_DELETE = 6;
    private static final int OP_LOG_TYPE_START = 7;
    private static final int OP_LOG_TYPE_STANDBY = 8;

    @Autowired
    @Qualifier("Scheduler")
    private Scheduler scheduler;
    @Autowired
    private JobDao jobDao;
    @Autowired
    private RunLogDao runLogDao;
    @Autowired
    private OpLogDao opLogDao;
    @Value("${flink.rest.baseUrl}")
    private String flinkRestBaseUrl;
    @Value("${flink.rest.httpAuth}")
    private String flinkRestHttpAuth;
    @Value("${flink.web.baseUrl}")
    private String flinkWebBaseUrl;

    public Map<String, Object> list(Map<String, Object> params) {
        Specification<com.iisquare.fs.web.quartz.entity.Job> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String name = DPUtil.trim(DPUtil.parseString(params.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("key").get("name"), "%" + name + "%"));
            }
            String group = DPUtil.trim(DPUtil.parseString(params.get("group")));
            if (!DPUtil.empty(group)) {
                predicates.add(cb.like(root.get("key").get("group"), "%" + group + "%"));
            }
            predicates.add(cb.notLike(root.get("key").get("name"), "%" + RUN_MANUAL_SUFFIX + "%"));
            Predicate[] predicate = new Predicate[predicates.size()];
            return cb.and(predicates.toArray(predicate));
        };
        int page = ValidateUtil.filterInteger(params.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(params.get("pageSize"), true, 1, 100, 15);
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        Page<com.iisquare.fs.web.quartz.entity.Job> jobPage = jobDao.findAll(specification, pageRequest);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", page);
        result.put("pageSize", pageSize);
        for (com.iisquare.fs.web.quartz.entity.Job job : jobPage.getContent()) {
            com.iisquare.fs.web.quartz.entity.JobKey key = job.getKey();
            try {
                job.setParams(DPUtil.stringify(scheduler.getJobDetail(JobKey.jobKey(key.getName(), key.getGroup())).getJobDataMap()));
                fillTriggerInfo(job);
            } catch (SchedulerException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        result.put("total", jobPage.getTotalElements());
        result.put("rows", jobPage.getContent());
        return result;
    }

    public Map<String, Object> map(List<JobKey> keys) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (JobKey key : keys) {
            String name = key.getName();
            String group = key.getGroup();
            try {
                JobDetail jobDetail = scheduler.getJobDetail(JobKey.jobKey(name, group));
                if (jobDetail == null) {
                    continue;
                }
                com.iisquare.fs.web.quartz.entity.Job job = new com.iisquare.fs.web.quartz.entity.Job();
                com.iisquare.fs.web.quartz.entity.JobKey jobKey = new com.iisquare.fs.web.quartz.entity.JobKey();
                jobKey.setSchedName(scheduler.getSchedulerName());
                jobKey.setName(name);
                jobKey.setGroup(group);
                job.setKey(jobKey);
                job.setDesc(jobDetail.getDescription());
                job.setJobClass(jobDetail.getJobClass().getName());
                job.setParams(DPUtil.stringify(jobDetail.getJobDataMap()));
                fillTriggerInfo(job);
                result.put(group + "." + name, job);
            } catch (SchedulerException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return result;
    }

    public Map add(String name, String group, String cron, String jobClass, Map<String, Object> params, String desc, int uid, boolean isModify) {
        Map<String, Object> result = new LinkedHashMap<>();
        JobKey jobKey = JobKey.jobKey(name, group);
        int opType;
        try {
            if (isModify) {
                JobDetail job = scheduler.getJobDetail(jobKey);
                if (job == null) {
                    opType = OP_LOG_TYPE_ADD;
                } else {
                    //删除旧job，这样可以更新jobDataMap
                    if (!scheduler.deleteJob(jobKey)) {
                        result.put("code", 500);
                        result.put("message", "删除旧作业失败");
                        return result;
                    }
                    opType = OP_LOG_TYPE_MODIFY;
                }
            } else {
                opType = OP_LOG_TYPE_ADD;
            }
            JobBuilder jobBuilder;
            if (jobClass.equals(JOB_CLASS_FLINK)) {
                jobBuilder = JobBuilder.newJob(FlinkJob.class);
            } else {
                result.put("code", 500);
                result.put("message", "作业类型非法");
                return result;
            }
            JobDetail jobDetail = jobBuilder
                    .withIdentity(jobKey)
                    .withDescription(desc)
                    .storeDurably()
                    .build();
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            jobDataMap.putAll(params);
            if (cron.isEmpty()) {
                scheduler.addJob(jobDetail, true);
            } else {
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
                Trigger trigger = TriggerBuilder
                        .newTrigger()
                        .withIdentity(name, group)
                        //忽略misfire，执行下一个周期的任务
                        .withSchedule(scheduleBuilder.withMisfireHandlingInstructionDoNothing())
                        .build();
                scheduler.scheduleJob(jobDetail, trigger);
            }
            Map<String, Object> opLogParams = new LinkedHashMap<>();
            opLogParams.put("cron", cron);
            opLogParams.put("class", jobClass);
            opLogParams.put("data", jobDataMap);
            opLogParams.put("desc", desc);
            addOpLog(name, group, opType, DPUtil.stringify(opLogParams), uid);
        } catch (SchedulerException e) {
            LOGGER.error(e.getMessage(), e);
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
        result.put("code", 0);
        result.put("message", "操作成功");
        return result;
    }

    public List<JobKey> pause(List<JobKey> keys, int uid) {
        List<JobKey> jobKeys = new ArrayList<>();
        for (JobKey key : keys) {
            try {
                scheduler.pauseJob(key);
                jobKeys.add(key);
            } catch (SchedulerException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        addOpLogs(jobKeys, OP_LOG_TYPE_PAUSE, uid);
        return jobKeys;
    }

    public List<JobKey> resume(List<JobKey> keys, int uid) {
        List<JobKey> jobKeys = new ArrayList<>();
        for (JobKey key : keys) {
            try {
                scheduler.resumeJob(key);
                jobKeys.add(key);
            } catch (SchedulerException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        addOpLogs(jobKeys, OP_LOG_TYPE_RESUME, uid);
        return jobKeys;
    }

    public Map<String, Object> run(String name, String group, Map<String, Object> params, int uid) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (isStandby()) {
            result.put("code", 500);
            result.put("message", "暂停调度模式下禁止手动执行");
            return result;
        }
        try {
            JobDetail job = scheduler.getJobDetail(JobKey.jobKey(name, group));
            if (job == null) {
                result.put("code", 500);
                result.put("message", "作业不存在");
                return result;
            }
            JobKey jobKey = JobKey.jobKey(name + RUN_MANUAL_SUFFIX, group);
            JobDetail jobDetail = JobBuilder
                    .newJob(job.getJobClass())
                    .withIdentity(jobKey)
                    .storeDurably()
                    .build();
            jobDetail.getJobDataMap().putAll(params);
            scheduler.addJob(jobDetail, true);
            scheduler.triggerJob(jobKey);
        } catch (SchedulerException e) {
            LOGGER.error(e.getMessage(), e);
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
        addOpLog(name, group, OP_LOG_TYPE_RUN, DPUtil.stringify(params), uid);
        result.put("code", 0);
        result.put("message", "操作成功");
        return result;
    }

    public List<JobKey> delete(List<JobKey> keys, int uid) {
        List<JobKey> jobKeys = new ArrayList<>();
        for (JobKey key : keys) {
            try {
                if (scheduler.deleteJob(key)) {
                    jobKeys.add(key);
                }
            } catch (SchedulerException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        addOpLogs(jobKeys, OP_LOG_TYPE_DELETE, uid);
        return jobKeys;
    }

    public Map<String, Object> logs(Map<String, Object> params) {
        Specification<?> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String name = DPUtil.trim(DPUtil.parseString(params.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.equal(root.get("name"), name));
            }
            String group = DPUtil.trim(DPUtil.parseString(params.get("group")));
            if (!DPUtil.empty(group)) {
                predicates.add(cb.equal(root.get("group"), group));
            }
            Predicate[] predicate = new Predicate[predicates.size()];
            return cb.and(predicates.toArray(predicate));
        };
        int page = ValidateUtil.filterInteger(params.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(params.get("pageSize"), true, 1, 100, 15);
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "id")));
        String type = DPUtil.trim(DPUtil.parseString(params.get("type")));
        long total = 0L;
        List<?> rows = new ArrayList<>();
        Page<?> logPage;
        switch (type) {
            case "op":
                logPage = opLogDao.findAll((Specification<OpLog>) specification, pageRequest);
                total = logPage.getTotalElements();
                rows = logPage.getContent();
                break;
            case "run":
                logPage = runLogDao.findAll((Specification<RunLog>) specification, pageRequest);
                total = logPage.getTotalElements();
                rows = logPage.getContent();
                //更新执行结果
                updateRunLogResult(rows);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.put("rows", rows);
        return result;
    }

    public RunLog log(String tag) {
        RunLog runLog = runLogDao.findByTag(tag);
        if (runLog != null) {
            List<RunLog> list = new ArrayList<>();
            list.add(runLog);
            updateRunLogResult(list);
        }
        return runLog;
    }

    public Map<String, String> jobClassConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put(JOB_CLASS_FLINK, "Flink");
        return config;
    }

    public Map<Trigger.TriggerState, String> statusConfig() {
        Map<Trigger.TriggerState, String> config = new LinkedHashMap<>();
        config.put(Trigger.TriggerState.NONE, "");
        config.put(Trigger.TriggerState.NORMAL, "正常");
        config.put(Trigger.TriggerState.PAUSED, "暂停");
        config.put(Trigger.TriggerState.COMPLETE, "完成");
        config.put(Trigger.TriggerState.ERROR, "错误");
        config.put(Trigger.TriggerState.BLOCKED, "阻塞");
        return config;
    }

    public Map<Integer, String> runLogTypeConfig() {
        Map<Integer, String> config = new LinkedHashMap<>();
        config.put(RUN_LOG_TYPE_NORMAL, "正常");
        config.put(RUN_LOG_TYPE_MANUAL, "手动");
        return config;
    }

    public Map<Integer, String> runLogResultConfig() {
        Map<Integer, String> config = new LinkedHashMap<>();
        config.put(RUN_LOG_RESULT_SUBMIT_FAILED, "提交失败");
        config.put(RUN_LOG_RESULT_RUNNING, "执行中");
        config.put(RUN_LOG_RESULT_FINISHED, "执行成功");
        config.put(RUN_LOG_RESULT_FAILED, "执行失败");
        config.put(RUN_LOG_RESULT_CANCELED, "取消");
        return config;
    }

    public Map<Integer, String> opLogTypeConfig() {
        Map<Integer, String> config = new LinkedHashMap<>();
        config.put(OP_LOG_TYPE_ADD, "添加");
        config.put(OP_LOG_TYPE_MODIFY, "编辑");
        config.put(OP_LOG_TYPE_PAUSE, "暂停");
        config.put(OP_LOG_TYPE_RESUME, "恢复");
        config.put(OP_LOG_TYPE_RUN, "手动执行");
        config.put(OP_LOG_TYPE_DELETE, "删除");
        config.put(OP_LOG_TYPE_START, "启动");
        config.put(OP_LOG_TYPE_STANDBY, "暂停");
        return config;
    }

    public boolean start(String host, int uid) {
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        addOpLog(host, "", OP_LOG_TYPE_START, "", uid);
        return true;
    }

    public boolean standby(String host, int uid) {
        try {
            scheduler.standby();
        } catch (SchedulerException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        addOpLog(host, "", OP_LOG_TYPE_STANDBY, "", uid);
        return true;
    }

    public boolean isStandby() {
        try {
            return scheduler.isInStandbyMode();
        } catch (SchedulerException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    public void addRunLog(JobExecutionContext context, int result, String oriResult) {
        JobDetail jobDetail = context.getJobDetail();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        JobKey jobKey = jobDetail.getKey();
        String name = jobKey.getName();
        int runLogType;
        if (name.contains(JobService.RUN_MANUAL_SUFFIX)) {
            name = name.replace(JobService.RUN_MANUAL_SUFFIX, "");
            runLogType = JobService.RUN_LOG_TYPE_MANUAL;
        } else {
            runLogType = JobService.RUN_LOG_TYPE_NORMAL;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        Trigger trigger = context.getTrigger();
        if (trigger instanceof CronTrigger) {
            params.put("cron", ((CronTrigger) trigger).getCronExpression());
        }
        params.put("data", jobDataMap);
        String node;
        try {
            node = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            node = "";
        }
        RunLog runLog = new RunLog();
        runLog.setTag(DPUtil.parseString(jobDataMap.get("runLogTag")));
        runLog.setName(name);
        runLog.setGroup(jobKey.getGroup());
        runLog.setJobClass(jobDetail.getJobClass().getName());
        runLog.setParams(DPUtil.stringify(params));
        runLog.setNode(node);
        runLog.setType(runLogType);
        runLog.setResult(result);
        runLog.setOriResult(oriResult);
        runLog.setCtime(System.currentTimeMillis());
        runLogDao.save(runLog);
    }

    public void updateRunLogResult(List<?> runLogs) {
        RestTemplate restTemplate = new RestTemplate();
        String restUrlPre = flinkRestBaseUrl + "jobs/";
        String webUrlPre = flinkWebBaseUrl + "jobs/";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", flinkRestHttpAuth);
        HttpEntity<String> requestEntity = new HttpEntity<>(null, httpHeaders);
        StringBuilder warnContent = new StringBuilder();
        for (Object item : runLogs) {
            RunLog runLog = (RunLog) item;
            int result = runLog.getResult();
            if (!runLog.getJobClass().equals(JOB_CLASS_FLINK) || result == RUN_LOG_RESULT_SUBMIT_FAILED) {
                continue;
            }
            JsonNode params = DPUtil.parseJSON(runLog.getParams());
            if (params == null) {
                continue;
            }
            String jobId = params.findPath("data").findPath("jobId").asText();
            if (jobId.isEmpty()) {
                continue;
            }
            String webUrl = webUrlPre + jobId;
            if (result == RUN_LOG_RESULT_RUNNING) {
                JsonNode job;
                try {
                    job = DPUtil.parseJSON(restTemplate.exchange(restUrlPre + jobId, HttpMethod.GET, requestEntity, String.class).getBody());
                } catch (RestClientException e) {
                    job = null;
                }
                if (job == null) {
                    continue;
                }
                int code = 0;
                String message = "";
                switch (job.findPath("state").asText()) {
                    case "FINISHED":
                        result = RUN_LOG_RESULT_FINISHED;
                        message = "作业执行成功";
                        break;
                    case "FAILED":
                        result = RUN_LOG_RESULT_FAILED;
                        code = 500;
                        message = "作业执行失败";
                        warnContent.append(webUrl).append("<br />");
                        break;
                    case "CANCELED":
                        result = RUN_LOG_RESULT_CANCELED;
                        message = "作业被取消";
                        break;
                }
                if (result > RUN_LOG_RESULT_RUNNING) {
                    runLog.setResult(result);
                    ObjectNode oriResult, data;
                    try {
                        oriResult = (ObjectNode) DPUtil.parseJSON(runLog.getOriResult());
                        if (oriResult != null && oriResult.has("data")) {
                            data = (ObjectNode) oriResult.get("data");
                        } else {
                            oriResult = DPUtil.objectNode();
                            data = oriResult.putObject("data");
                        }
                    } catch (Exception e) {
                        oriResult = DPUtil.objectNode();
                        data = oriResult.putObject("data");
                    }
                    oriResult.put("code", code);
                    oriResult.put("message", message);
                    data.set("job", job);
                    runLog.setOriResult(DPUtil.stringify(oriResult));
                    runLogDao.save(runLog);
                }
            }
            if (result > RUN_LOG_RESULT_RUNNING) {
                runLog.setUrl(webUrl);
            }
        }
    }

    private void fillTriggerInfo(com.iisquare.fs.web.quartz.entity.Job job) throws SchedulerException {
        com.iisquare.fs.web.quartz.entity.JobKey jobKey = job.getKey();
        Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
        if (trigger == null) {
            return;
        }
        if (trigger instanceof CronTrigger) {
            job.setCron(((CronTrigger) trigger).getCronExpression());
        }
        job.setStatus(scheduler.getTriggerState(trigger.getKey()));
        job.setUtime(trigger.getStartTime());
    }

    private void addOpLog(String name, String group, int type, String params, int uid) {
        OpLog opLog = new OpLog();
        opLog.setName(name);
        opLog.setGroup(group);
        opLog.setType(type);
        opLog.setParams(params);
        opLog.setCuid(uid);
        opLog.setCtime(System.currentTimeMillis());
        opLogDao.save(opLog);
    }

    private void addOpLogs(List<JobKey> jobKeys, int type, int uid) {
        if (jobKeys.isEmpty()) {
            return;
        }
        List<OpLog> opLogs = new ArrayList<>();
        String params = "";
        long ctime = System.currentTimeMillis();
        for (JobKey jobKey : jobKeys) {
            OpLog opLog = new OpLog();
            opLog.setName(jobKey.getName());
            opLog.setGroup(jobKey.getGroup());
            opLog.setType(type);
            opLog.setParams(params);
            opLog.setCuid(uid);
            opLog.setCtime(ctime);
            opLogs.add(opLog);
        }
        opLogDao.saveAll(opLogs);
    }
}
