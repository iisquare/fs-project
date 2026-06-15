package com.iisquare.fs.web.lm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Usage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private Integer uid; // 用户
    @Column
    private String type; // 计费类型：充值、消费等
    @Column
    private String place; // 充值消费地点，如充值平台、调用的模型名称等
    @Column
    private BigDecimal creditAmount; // 积分数量，消费为负值
    @Column
    private String status; // 执行状态
    /* 模型通用字段 */
    @Column
    private Integer authId; // 密钥
    @Column
    private Integer modelId; // 模型
    @Column
    private Integer providerId; // 供应商
    @Column
    private String requestIp; // 调用端地址
    @Column
    private Long beginTime; // 处理开始时间
    @Column
    private Long endTime; // 处理结束时间
    @Column
    private Integer coastTotal; // 整体耗时（毫秒）
    /* 对话模型字段 */
    @Column
    private String requestBody; // 请求参数
    @Column
    private Integer requestStream; // 是否为流式输出
    @Column
    private String requestPrompt; // 完整提示词
    @Column
    private String requestSystem; // 系统提示词
    @Column
    private String requestUser; // 用户提问
    @Column
    private String responseBody; // 完整响应内容
    @Column
    private String responseReason; // 思考
    @Column
    private String responseCompletion; // 回答
    @Column
    private String responseTool; // 工具调用
    @Column
    private String finishReason; // 完成状态，含正常输出完成、输出长度达到限制、客户端停止、敏感词拦截
    @Column
    private String finishDetail; // 详细原因
    @Column
    private Integer usagePromptCachedTokens;
    @Column
    private Integer usagePromptTokens; // 总输入：usagePromptCacheHitTokens + usagePromptCacheMissTokens
    @Column
    private Integer usageCompletionTokens; // 总输出
    @Column
    private Integer usageTotalTokens; // 总使用：usagePromptTokens + usageCompletionTokens
    /* 管理功能字段 */
    @Column
    private String auditReason; // 审核原因
    @Column
    private String auditDetail; // 审核描述
    @Column
    private Long auditTime; // 审核时间
    @Column
    private Integer auditUid; // 审核人员
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

    public static class UsageBuilder {

        public String getFinishReason() {
            return finishReason;
        };

    }

}
