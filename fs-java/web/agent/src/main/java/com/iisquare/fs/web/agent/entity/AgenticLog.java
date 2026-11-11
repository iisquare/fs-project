package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 编排运行日志：每次调试运行（草稿）或外部调用（发布内容）落一条，一轮对话一条。
 *
 * - 归属：agenticId 指向编排应用（Agentic），chatId 指向会话（AgenticChat），均为列关联、无外键约束；
 * - answerId 指向本轮助手回复（AgenticDialog.id）：会话回看按该列把「回复 ↔ 运行明细」对上，
 *   不再解析 outputs JSON；
 * - 大字段（列表接口不返回，详情与「执行过程」按 logId 懒加载）：
 *   inputs 运行入参、outputs 运行输出（写入时已去掉与 inputs / steps 重复的内容）、
 *   steps 逐节点状态与输入输出摘要、error 完整失败原因（保留堆栈，不截断）；
 * - failures 为写入时预计算的异常摘要（JSON 数组，节点失败与工具方法调用失败，
 *   最多 5 条、单条截断），会话回看直接展示异常图标；
 * - 调用来源：createdUid 是最终生效用户，apiKeyId 关联 agent 外部调用 Key；
 *   名称与属主在查询时按 apiKeyId 填充，callerType 与 userProxy 标识调用方式；
 * - traceId 同时写入 LM 用量，externalUserId 仅作为外部终端用户审计标识，不参与鉴权；
 * - 日志写入失败不影响运行结果，仅记录 warn 日志；删除为标记删除（deletedTime=0 表示未删除）。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class AgenticLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer agenticId; // 编排标识
    @Column
    private Integer chatId; // 所属会话（多轮对话时用于按会话回看每次运行的明细）
    @Column
    private Integer answerId; // 本轮助手回复的消息标识（fs_agent_agentic_dialog.id），回看会话时直接按列关联
    @Column
    private String source; // 来源：draft-调试运行，published-外部调用
    @Column
    private Integer version; // 发布版本，调试运行固定为 0
    @Column
    private Integer status; // 1-成功，2-失败
    @Column
    private Long duration; // 执行耗时（毫秒）
    @Column
    private String inputs; // 运行入参
    @Column
    private String outputs; // 运行输出（含回复内容 answer）
    @Column
    private String steps; // 逐节点执行明细
    @Column
    private String error; // 失败原因
    @Column
    private String failures; // 异常摘要（JSON 数组，写入时预计算）：会话回看直接展示，不必再解析 steps
    @Column
    private String ip; // 调用来源 IP
    @Column
    private Integer apiKeyId; // agent 外部调用密钥
    @Column
    private String callerType; // session / api_key / user_proxy
    @Column
    private Integer userProxy; // 是否使用用户会话代理：1-是，0-否
    @Column
    private String externalUserId; // 外部系统终端用户标识，仅审计，不参与鉴权
    @Column
    private String traceId; // 贯穿 agent 与 LM 用量记录的追踪标识
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long deletedTime; // 删除时间，0 表示未删除（标记删除）
    @Column
    private Integer deletedUid;

}
