package com.iisquare.fs.web.cron.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

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
@IdClass(FlowStage.IdClass.class)
public class FlowStage implements Serializable {

    public enum State {
        WAITING, // 等待调度
        RUNNING, // 正在执行
        SKIPPED, // 跳过执行
        SUCCEED, // 执行成功
        FAILED, // 执行失败
        TERMINATED, // 执行终止
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private Integer logId;
        private String stageId;
    }

    @Id
    private Integer logId; // 所属日志
    @Id
    private String stageId; // 所属节点
    @Column
    private String parentId; // 所属子流程
    @Column
    private String type; // 任务类型
    @Column
    private String name; // 任务名称
    @Column
    private String title; // 任务标题
    @Column
    private Integer skipped; // 是否跳过执行
    @Column
    private String depend; // 执行依赖，英文逗号分割
    @Column
    private String data; // 节点参数
    @Column
    private String content; // 输出结果
    @Column
    private String state; // 运行状态
    @Column
    private Long createdTime;
    @Column
    private Long runTime; // 开始执行时间
    @Column
    private Long updatedTime;

    @Transient
    private Long duration; // 持续时间
    @Transient
    private String durationPretty; // 持续时间美化

    public static boolean isFinished(String state) {
        return !Arrays.asList(State.WAITING.name(), State.RUNNING.name()).contains(state);
    }

    public static boolean isSucceed(String state) {
        return Arrays.asList(State.SKIPPED.name(), State.SUCCEED.name()).contains(state);
    }

    public static boolean isFailed(String state) {
        return Arrays.asList(State.FAILED.name(), State.TERMINATED.name()).contains(state);
    }

}
