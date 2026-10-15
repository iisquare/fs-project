package com.iisquare.fs.web.bi.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 计算作业运行记录：既用于流批作业的执行追踪，也用于运行状态监控与预警
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Job {

    public static final String STATE_SUBMITTING = "SUBMITTING";
    public static final String STATE_RUNNING = "RUNNING";
    public static final String STATE_FINISHED = "FINISHED";
    public static final String STATE_FAILED = "FAILED";
    public static final String STATE_CANCELLED = "CANCELLED";
    public static final String STATE_UNKNOWN = "UNKNOWN";

    public static final String TRIGGER_MANUAL = "manual";
    public static final String TRIGGER_SCHEDULE = "schedule";
    public static final String TRIGGER_DEBUG = "debug";
    public static final String TRIGGER_RECOVER = "recover";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer diagramId; // 编排流程
    @Column
    private Integer clusterId; // 计算集群
    @Column
    private String name; // 作业名称
    @Column
    private String engine; // spark / flink
    @Column
    private String model; // batch / stream
    @Column
    private String triggerType; // 触发方式
    @Column
    private String jobId; // 远程作业标识
    @Column
    private String state; // 运行状态
    @Column
    private Integer retry; // 已自动恢复次数
    @Column
    private Integer autoRecover; // 是否启用自动恢复
    @Column
    private Integer notified; // 是否已发送预警
    @Column
    private String alarm; // 预警信息
    @Column
    private String data; // 运行参数
    @Column(columnDefinition = "longtext")
    private String content; // 编排快照
    @Column(columnDefinition = "longtext")
    private String outputs; // 调试输出结果
    @Column(columnDefinition = "text")
    private String result; // 提交/状态原始结果
    @Column
    private Long startTime;
    @Column
    private Long endTime;
    @Transient
    private String stateText;
    @Transient
    private String triggerTypeText;
    @Transient
    private String engineText;
    @Transient
    private String modelText;
    @Column
    private Integer createdUid;
    @Column
    private Long createdTime;
    @Column
    private Integer updatedUid;
    @Column
    private Long updatedTime;

}
