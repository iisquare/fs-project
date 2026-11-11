package com.iisquare.fs.web.agent.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 编排应用（智能体工作流）：画布内容分草稿与发布内容两套，互不影响。
 *
 * - content：草稿内容（画布 JSON），保存后供设计器与调试运行（/run、/runStream）使用；
 * - publishedContent：发布时固化的发布内容，外部调用（/invoke）只读它；
 *   发布时会把工具方法定义快照进去，之后工具变动不影响已发布版本；
 * - roleIds：授权角色，为空表示所有登录用户可用；对话页只列出「已发布 + 状态启用 + 授权命中」的应用；
 * - 运行记录见 AgenticLog，对话记录见 AgenticChat / AgenticDialog。
 */
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
    private String roleIds; // 授权角色（逗号分隔），为空表示所有登录用户可用
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
