package com.iisquare.fs.web.ai.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class LmLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer clientId;
    @Column
    private Integer clientEndpointId;
    @Column
    private Integer serverId;
    @Column
    private Integer serverEndpointId;
    @Column
    private String request; // 请求参数
    @Column
    private String response; // 响应内容
    @Column
    private String requestIp; // 调用端地址
    @Column
    private String responseState; // 响应状态
    @Column
    private String finishReason; // 完成原因，含正常输出完成、输出长度达到限制、客户端停止、敏感词拦截
    @Column
    private Integer requestStream; // 是否为流式输出
    @Column
    private String responseId;
    @Column
    private Integer usagePromptTokens;
    @Column
    private Integer usageCompletionTokens;
    @Column
    private Integer usageTotalTokens;
    @Column
    private Long beginTime; // 请求开始时间
    @Column
    private Long firstTime; // 首Token时间
    @Column
    private Long endTime; // 请求结束时间

}
