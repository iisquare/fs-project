package com.iisquare.fs.web.cron.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.quartz.JobKey;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class FlowLog implements Serializable {

    public enum State {
        MISSING, // 流程丢失
        SKIPPED, // 跳过执行
        RUNNING, // 正在执行
        SUCCEED, // 执行成功
        FAILED, // 执行失败
        TERMINATED, // 存在循环依赖或任务失败终止
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String project; // 项目名称
    @Column
    private String name; // 流程名称
    @Column
    private Integer concurrent; // 并发数量
    @Column
    private String concurrency; // 并发策略
    @Column
    private String failure; // 失败策略
    @Column
    private String data; // 全局参数
    @Column
    private String content; // 流程快照
    @Column
    private String state; // 运行状态
    @Column
    private Long createdTime;
    @Column
    private Long updatedTime;

    @Transient
    private Long duration; // 持续时间
    @Transient
    private String durationPretty; // 持续时间美化

    public static FlowLog missing(JobKey jobKey, long time) {
        return FlowLog.builder()
                .project(jobKey.getGroup()).name(jobKey.getName())
                .concurrent(0).concurrency("").failure("")
                .data("").content("").state(State.MISSING.name())
                .createdTime(time).updatedTime(time)
                .build();
    }

    public static boolean isFinished(String state) {
        return !Arrays.asList(State.RUNNING.name()).contains(state);
    }

    public static boolean isSucceed(String state) {
        return Arrays.asList(State.SKIPPED.name(), State.SUCCEED.name()).contains(state);
    }

    public static boolean isFailed(String state) {
        return Arrays.asList(State.MISSING.name(), State.FAILED.name(), State.TERMINATED.name()).contains(state);
    }

}
