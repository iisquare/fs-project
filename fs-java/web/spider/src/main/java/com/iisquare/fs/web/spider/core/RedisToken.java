package com.iisquare.fs.web.spider.core;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * 令牌环Token
 * 采用Redis.ZSet的Score值处理任务优先级
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RedisToken {

    public String id; // 用于重复写入Redis
    public String jobId; // 所属作业
    public Long score; // 令牌优先级，由调度器自动设置
    public Long rotateCount; // 轮转次数
    public Long effectiveCount; // 有效处理任务的次数
    public Integer ineffectiveCount; // 连续无效获取任务计数
    public Long versionTime; // 对应作业的状态变更时间
    public Long lastTime; // 最后调度时间

    public static String encode(RedisToken token) {
        return DPUtil.stringify(DPUtil.toJSON(token));
    }

    public static RedisToken decode(String token) {
        return DPUtil.toJSON(DPUtil.parseJSON(token), RedisToken.class);
    }

    public static RedisToken record(ZooJob job) {
        RedisToken token = new RedisToken();
        token.id = UUID.randomUUID().toString();
        token.jobId = job.getId();
        token.versionTime = job.getOperatingTime();
        token.score = job.score(false);
        token.rotateCount = 0L;
        token.effectiveCount = 0L;
        token.ineffectiveCount = 0;
        token.lastTime = System.currentTimeMillis();
        return token;
    }

    /**
     * 停顿指定时长
     */
    public RedisToken halt(long halt) {
        long time = System.currentTimeMillis();
        this.score = time + halt;
        this.lastTime = time;
        this.rotateCount++;
        return this;
    }

    public RedisToken back(ZooJob job) {
        this.score = job.score(false);
        this.lastTime = System.currentTimeMillis();
        this.rotateCount++;
        return this;
    }

}
