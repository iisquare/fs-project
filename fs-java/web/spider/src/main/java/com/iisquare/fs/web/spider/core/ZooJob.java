package com.iisquare.fs.web.spider.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Data;

@Data
public class ZooJob {

    public enum Status {

        CREATED("已创建"),
        RUNNING("运行中"),
        STOP("已停止"),
        PAUSE("已暂停"),
        FINISHED("已完成"),
        ;

        private final String text;

        Status (String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    public String id;
    public JsonNode template;
    public Status status;
    public Long createdTime;
    public Long updatedTime;
    public Long finishedTime;
    public Long operatingTime; // 变更作业状态的时间，用于控制Token过期

    public static String encode(ZooJob job) {
        return DPUtil.stringify(DPUtil.toJSON(job));
    }

    public static ZooJob decode(String job) {
        return DPUtil.toJSON(DPUtil.parseJSON(job), ZooJob.class);
    }

    public long score(boolean bHalt) {
        long score = template.at("/priority").asLong();
        if (score < 1) {
            score = System.currentTimeMillis(); // 采用当前时间作为优先级
        }
        if (bHalt) score += halt();
        return score;
    }

    /**
     * 获取停顿时长
     */
    public int halt() {
        int minHalt = template.at("/minHalt").asInt();
        int maxHalt = template.at("/maxHalt").asInt();
        return DPUtil.random(minHalt, maxHalt);
    }

}
