package com.iisquare.fs.web.spider.core;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.spider.entity.Rate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 请求频率限制
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZooRate {

    public String id;
    public String name;
    public Integer parallel; // 并行度
    public Integer concurrent; // 并发数
    public Integer interval; // 并发间隔，毫秒
    public Boolean perJob; // 每作业
    public Boolean perDomain; // 每域名
    public Boolean perNode; // 每节点
    public Boolean perProxy; // 每代理
    public Boolean haltToken; // 延迟Token令牌
    public Boolean haltTask; // 延迟Task任务

    public static String encode(ZooRate token) {
        return DPUtil.stringify(DPUtil.toJSON(token));
    }

    public static ZooRate decode(String token) {
        return DPUtil.toJSON(DPUtil.parseJSON(token), ZooRate.class);
    }

    public static ZooRate record(Rate info) {
        ZooRate rate = new ZooRate();
        rate.id = String.valueOf(info.getId());
        rate.name = info.getName();
        rate.parallel = info.getParallelByKey();
        rate.concurrent = info.getConcurrent();
        rate.interval = info.getIntervalMillisecond();
        rate.perJob = DPUtil.parseBoolean(info.getPerJob());
        rate.perDomain = DPUtil.parseBoolean(info.getPerDomain());
        rate.perNode = DPUtil.parseBoolean(info.getPerNode());
        rate.perProxy = DPUtil.parseBoolean(info.getPerProxy());
        rate.haltToken = DPUtil.parseBoolean(info.getHaltToken());
        rate.haltTask = DPUtil.parseBoolean(info.getHaltTask());
        return rate;
    }

}
