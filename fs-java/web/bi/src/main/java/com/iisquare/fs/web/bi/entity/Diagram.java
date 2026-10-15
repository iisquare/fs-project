package com.iisquare.fs.web.bi.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Diagram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private String engine; // 计算引擎
    @Transient
    private String engineText;
    @Column
    private String model; // 处理模式
    @Transient
    private String modelText;
    @Column
    private Integer clusterId; // 计算集群
    @Transient
    private String clusterIdName;
    @Column
    private String content;
    @Column
    private String expression; // 定时调度Cron表达式
    @Column
    private Integer schedule; // 是否启用定时调度，1-启用，0-禁用
    @Column
    private String notify; // 消息通知配置
    @Column
    private Integer autoRecover; // 是否自动恢复，1-是，0-否
    @Column
    private Integer maxRetry; // 自动恢复最大次数
    @Column
    private Long lastTime; // 最近调度时间
    @Column
    private Long nextTime; // 下次调度时间
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Transient
    private String statusText;
    @Column
    private String description;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Transient
    private String createdUidName;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;
    @Transient
    private String updatedUidName;

}
