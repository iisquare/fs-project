package com.iisquare.fs.web.spider.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 泛采集单个站点的策略配置
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Site implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer templateId; // 所属模板标识
    @Column
    private String name; // 名称
    @Column
    private String domain; // 域名，“*”为默认兜底策略
    @Column
    private String charset; // 页面编码，默认utf-8
    @Column
    private String collection; // 采集结果存储到Mongo中的集合名称
    @Column
    private String bucket; // 附件存储到MinIO中的桶名称
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
