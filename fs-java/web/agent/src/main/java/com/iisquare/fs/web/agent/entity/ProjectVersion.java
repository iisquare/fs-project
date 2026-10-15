package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 分享项目的版本：一条记录 = 一次发布的项目配置快照。
 *
 * content 是客户端提交的项目配置原文（JSON 文本），服务端不解释它的结构，
 * 客户端按它导出配置文件或创建本地项目；版本号（name）与版本说明（description）由发布人填写。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class ProjectVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer projectId; // 所属分享项目
    @Column
    private String name; // 版本号
    @Column
    private String content; // 项目配置内容（客户端提交的 JSON 原文）
    @Column
    private Integer status; // 1-启用，2-停用（停用后其它用户不再可见）
    @Column
    private String description; // 版本说明
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
