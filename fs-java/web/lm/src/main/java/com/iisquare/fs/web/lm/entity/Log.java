package com.iisquare.fs.web.lm.entity;

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
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private Integer clientId;
    @Column
    private Integer clientEndpointId;
    @Column
    private Integer serverId;
    @Column
    private Integer serverEndpointId;

    @Column
    private String requestBody; // 请求参数
    @Column
    private String requestIp; // 调用端地址
    @Column
    private Integer requestStream; // 是否为流式输出
    @Column
    private String requestPrompt; // 问题
    @Column
    private String responseBody; // 响应内容
    @Column
    private String responseCompletion; // 回答
    @Column
    private String finishReason; // 完成状态，含正常输出完成、输出长度达到限制、客户端停止、敏感词拦截
    @Column
    private String finishDetail; // 详细原因

    @Column
    private Integer usagePromptTokens;
    @Column
    private Integer usageCompletionTokens;
    @Column
    private Integer usageTotalTokens;

    @Column
    private String auditReason; // 审核原因
    @Column
    private String auditDetail; // 审核描述

    @Column
    private Long beginTime; // 处理开始时间
    @Column
    private Long requestTime; // 请求开始时间
    @Column
    private Long waitingTime; // 等待首次响应时间
    @Column
    private Long responseTime; // 请求结束时间
    @Column
    private Long endTime; // 处理结束时间
    @Column
    private Long auditTime; // 审核时间
    @Column
    private Integer auditUid; // 审核人员
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

}
