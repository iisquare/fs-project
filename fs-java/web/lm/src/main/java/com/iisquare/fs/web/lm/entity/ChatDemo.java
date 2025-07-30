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
public class ChatDemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer chatId;
    @Column
    private Integer agentId;
    @Column
    private String systemPrompt; // 系统提示词
    @Column
    private Integer maxTokens; // 要生成的最大令牌数，为0不传递该参数
    @Column
    private Float temperature; // 多样性
    @Column
    private String requestPrompt; // 问题
    @Column
    private String responseCompletion; // 回答
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

}
