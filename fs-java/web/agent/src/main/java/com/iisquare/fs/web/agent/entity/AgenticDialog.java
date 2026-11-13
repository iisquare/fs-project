package com.iisquare.fs.web.agent.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 会话消息：一轮对话落两条 —— 用户提问（role=user）与助手回复（role=assistant）。
 *
 * - 只存「展示与多轮记忆」需要的字段：content 正文、reasoningContent 思考过程、
 *   reference 参考数据（用户消息按模型节点保存的上下文、助手工具调用明细与图表）、feedback* 点赞点踩；
 * - parentId 组成消息树：0 表示分支起点，其余指向上一轮消息。
 *   编辑提问或重新生成回复时新起分支，记忆按会话的 leafId 沿 parentId 回溯取；
 * - 运行明细（入参、节点与工具调用过程）单独存 agentic_log，按 agentic_log.answer_id 关联本轮回复，
 *   前端展开「执行过程」时才按 logId 懒加载；
 * - 删除为标记删除：deletedTime=0 表示未删除，删除会话时随会话一并打标记。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class AgenticDialog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer chatId; // 所属会话
    @Column
    private Integer parentId; // 父消息：0 表示分支起点，其余指向上一轮消息
    @Column
    private String role; // 角色：user-用户，assistant-助手
    @Column
    private String reasoningContent; // 思考过程
    @Column
    private String content; // 消息内容
    @Column
    private String reference; // 参考数据（用户消息上下文、工具调用明细与图表）
    @Column
    private String feedbackEmotion; // 反馈情绪：positive-赞，negative-踩，cancel-取消
    @Column
    private String feedbackTag; // 反馈标签
    @Column
    private String feedbackContent; // 反馈内容
    @Column
    private Long feedbackTime; // 反馈时间，最后更新为准
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long deletedTime; // 删除时间，0 表示未删除（标记删除）
    @Column
    private Integer deletedUid; // 删除人，0-系统标记删除

}
