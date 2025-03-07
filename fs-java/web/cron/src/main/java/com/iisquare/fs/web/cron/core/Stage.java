package com.iisquare.fs.web.cron.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.cron.CronApplication;
import com.iisquare.fs.web.cron.dao.FlowLogDao;
import com.iisquare.fs.web.cron.dao.FlowStageDao;
import com.iisquare.fs.web.cron.entity.FlowStage;
import com.iisquare.fs.web.cron.service.FlowLogService;

import java.util.Map;

public abstract class Stage implements Runnable {

    public JsonNode stage; // 任务信息
    public ObjectNode config; // 全局配置
    public JsonNode options; // 节点配置

    public Stage() {}

    public static Stage getInstance(JsonNode stage, ObjectNode config) {
        String type = stage.at("/type").asText();
        try {
            Class cls = Class.forName(Stage.class.getPackage().getName().replaceFirst("core", String.format("stage.%sStage", type)));
            Stage instance = (Stage) cls.newInstance();
            instance.stage = stage;
            instance.config = config;
            JsonNode options = DPUtil.parseJSON(stage.at("/data").asText());
            instance.options = (null == options || !options.isObject()) ? DPUtil.objectNode() : options;
            return instance;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void run() {
        Map<String, Object> result = execute();
        int logId = stage.at("/logId").asInt();
        String stageId = stage.at("/stageId").asText();
        FlowLogService logService = CronApplication.context.getBean(FlowLogService.class);
        logService.notify(DPUtil.buildMap("logId", logId, "stageId", stageId, "code", result.get(ApiUtil.FIELD_CODE)), true);
    }

    public boolean inSucceed(JsonNode state, String... depends) {
        for (String depend : depends) {
            if (FlowStage.isFailed(state.at("/" + depend).asText())) {
                return false;
            }
        }
        return true;
    }

    public boolean isConfigStage() {
        return getClass().getSimpleName().endsWith("ConfigStage");
    }

    public boolean isGatewayStage() {
        return getClass().getSimpleName().endsWith("GatewayStage");
    }

    public Map<String, Object> execute() {
        int logId = stage.at("/logId").asInt();
        String stageId = stage.at("/stageId").asText();
        FlowLogDao logDao = CronApplication.context.getBean(FlowLogDao.class);
        FlowStageDao stageDao = CronApplication.context.getBean(FlowStageDao.class);
        String[] depend = DPUtil.explode(",", stage.at("/depend").asText());
        if (!isGatewayStage() && !inSucceed(config.at("/state"), depend)) { // 依赖节点异常
            Integer result = stageDao.finish(logId, stageId, FlowStage.State.TERMINATED.name(), System.currentTimeMillis());
            return ApiUtil.result(31001, "终止执行", result);
        }
        boolean skipped = stage.at("/skipped").asBoolean(false);
        if (skipped) {
            Integer result = stageDao.finish(logId, stageId, FlowStage.State.SKIPPED.name(), System.currentTimeMillis());
            return ApiUtil.result(31001, "跳过执行", result);
        }
        stageDao.running(logId, stageId, System.currentTimeMillis());
        Map<String, Object> result;
        try {
            result = call();
        } catch (Exception e) {
            result = ApiUtil.result(3511, "执行任务回调异常", ApiUtil.getStackTrace(e));
        }
        String content = ApiUtil.data(result, String.class);
        if (ApiUtil.succeed(result)) {
            if (isConfigStage()) {
                String data = DPUtil.stringify(config.setAll((ObjectNode) DPUtil.parseJSON(content)));
                if (!DPUtil.empty(data)) {
                    logDao.config(logId, data);
                }
            }
        }
        FlowStage.State state = ApiUtil.succeed(result) ? FlowStage.State.SUCCEED : FlowStage.State.FAILED;
        stageDao.finish(logId, stageId, content, state.name(), System.currentTimeMillis());
        return result;
    }

    public abstract Map<String, Object> call() throws Exception;

}
