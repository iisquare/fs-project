package com.iisquare.fs.web.bi.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 自定义 JAR 元数据。文件本体保存在文件服务的 fs-bi 桶中。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Jar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true)
    private String fileId; // 文件服务 archive id
    @Column
    private String name; // 原始文件名
    @Column
    private String bucket;
    @Column
    private String filepath;
    @Column
    private String sha256;
    @Column
    private Long size;
    @Column(columnDefinition = "longtext")
    private String nodes; // 扫描出的 DAGNode 元数据 JSON
    @Column
    private Integer status; // 1-可用，-1-已删除
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
