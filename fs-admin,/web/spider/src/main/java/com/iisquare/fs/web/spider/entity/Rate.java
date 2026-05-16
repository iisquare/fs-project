package com.iisquare.fs.web.spider.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 请求速率限制
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Rate implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 名称
    @Column
    private Integer parallelByKey; // 并行度
    @Column
    private Integer concurrent; // 并发数
    @Column
    private Integer intervalMillisecond; // 并发间隔，毫秒
    @Column
    private Integer perJob; // 每作业
    @Column
    private Integer perDomain; // 每域名
    @Column
    private Integer perNode; // 每节点
    @Column
    private Integer perProxy; // 每代理
    @Column
    private Integer haltToken; // 延迟Token令牌
    @Column
    private Integer haltTask; // 延迟Task任务
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Column
    private String description;
    @Column
    private Long publishedTime;
    @Column
    private Integer publishedUid;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
