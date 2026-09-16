package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 知识库图片：文档解析出的图谱、图表等媒体资源
 * 主键沿用文件服务的归档标识，便于与文件服务、对象存储对应
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class KnowledgeImage {

    @Id
    private String id; // 文件服务归档标识
    @Column
    private Integer knowledgeId; // 所属知识库
    @Column
    private Integer documentId; // 所属文档
    @Column
    private Integer segmentId; // 所属分段
    @Column
    private String bucket; // 存储桶
    @Column
    private String filepath; // 存储路径
    @Column
    private String name; // 原始文件名
    @Column
    private String suffix; // 文件后缀
    @Column
    private String type; // 文件类型
    @Column
    private Long size; // 文件大小
    @Column
    private String alt; // 图片说明，用于检索与无障碍展示
    @Column
    private Integer page; // 来源页码
    @Column
    private Integer sort; // 排序
    @Column
    private Integer status; // 状态：1-启用，2-禁用
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
