package com.iisquare.fs.web.agent.entity;

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
public class Agentic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 编排名称
    @Column
    private String mode; // 应用类型：workflow工作流、chat对话流
    @Column
    private String icon; // 应用图标
    @Column
    private String tags; // 应用标签，JSON数组
    @Column
    private String content; // 草稿内容（画布JSON），保存后仅用于调试运行
    @Column
    private String publishedContent; // 发布内容（画布JSON），外部调用使用
    @Column
    private Integer publishedVersion; // 发布版本，0表示未发布
    @Column
    private Long publishedTime; // 发布时间
    @Column
    private Integer publishedUid; // 发布人
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Column
    private String description;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
