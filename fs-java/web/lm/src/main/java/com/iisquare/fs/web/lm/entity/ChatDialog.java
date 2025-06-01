package com.iisquare.fs.web.lm.entity;

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
public class ChatDialog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer chatId;
    @Column
    private Integer parentId;
    @Column
    private String role; // 角色
    @Column
    private String reasoningContent; // 思考
    @Column
    private String content; // 内容
    @Column
    private String intent; // 意图识别结果
    @Column
    private String reference; // 参考数据
    @Column
    private String finishReason;
    @Column
    private String feedbackEmotion; // 反馈情绪，positive-赞，negative-踩，cancel-取消
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
    private String auditReason; // 审核原因
    @Column
    private String auditDetail; // 审核描述
    @Column
    private Long auditTime; // 审核时间
    @Column
    private Integer auditUid; // 审核人员


    @Column
    private String deletedReason; // 删除原因，同步删除所属会话
    @Column
    private String deletedDetail; // 删除描述
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid; // 用户自主删除，0-系统标记删除，人工审核删除

}
