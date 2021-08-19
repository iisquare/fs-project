package com.iisquare.fs.app.crawler.schedule;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Task {

    /**
     Parent *Task // 上级访问
     Session map[string]string // 会话状态
     Result map[string]interface{} // 执行结果
     Output output.Output // 输出
     */

    private String id;
    private String url;
    private String scheduleId;
    private String templateKey;
    private Long dispatchTime; // 加入调度时间
    private long priority; // Task优先级，设置调度间隔后无效
    private Integer dispatchInterval; // 调度暂停间隔
    private Integer retryCount; // 失败重试次数
    private Integer iterateCount; // 翻页迭代次数
    private Map<String, String> param; // 查询参数

    public Task(String scheduleId, String templateKey, Map<String, String> param) {
        this.id = UUID.randomUUID().toString();
        this.scheduleId = scheduleId;
        this.templateKey = templateKey;
        this.param = param;
    }

    public void reset(Scheduler scheduler) {
        Template template = template(scheduler);
        if (null == template) return;
        this.url = url(template.getUrl(), param);
        this.dispatchTime = System.currentTimeMillis();
        this.priority = template.getPriority();
        this.retryCount = 0;
        this.iterateCount = 0;
        this.dispatchInterval = dispatchInterval(scheduler);
    }

    public static String encode(Task task) {
        return DPUtil.stringify(DPUtil.toJSON(task));
    }

    public static Task decode(String task) {
        return DPUtil.toJSON(DPUtil.parseJSON(task), Task.class);
    }

    public Schedule schedule(Scheduler scheduler) {
        return scheduler.schedule(scheduleId);
    }

    public Template template(Scheduler scheduler) {
        return scheduler.template(scheduleId, templateKey);
    }

    public Task retry(Scheduler scheduler) {
        this.retryCount++;
        this.dispatchTime = System.currentTimeMillis();
        this.dispatchInterval = dispatchInterval(scheduler);
        return this;
    }

    public Task iterate(Scheduler scheduler, String url) {
        this.url = url;
        this.retryCount = 0;
        this.iterateCount++;
        this.dispatchTime = System.currentTimeMillis();
        this.dispatchInterval = dispatchInterval(scheduler);
        return this;
    }

    private String url(String url, Map<String, String> param) {
        if (null == url || null == param) return null;
        for (Map.Entry<String, String> entry : param.entrySet()) {
            url = url.replaceAll("\\{" + entry.getKey() + "\\}", entry.getValue());
        }
        return url;
    }

    private int dispatchInterval(Scheduler scheduler) {
        Template template = template(scheduler);
        int result = template.getMaxInterval() * (retryCount + 1) - template.getMinInterval();
        result *= Math.random();
        return result + template.getMinInterval();
    }

    public long score() {
        long interval = getDispatchInterval();
        if (interval == 0) {
            return getPriority();
        }
        return getDispatchTime() + interval;
    }

}
