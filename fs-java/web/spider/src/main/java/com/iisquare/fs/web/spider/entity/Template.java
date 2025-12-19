package com.iisquare.fs.web.spider.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Template implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 模板名称
    @Column
    private String type; // 模板类型
    @Column
    private Integer rateId; // 请求频率标识
    @Column
    private Integer maxThreads; // 最大线程数（向集群申请的线程资源数量，通过Token令牌控制）
    @Column
    private Long priority; // 优先级，默认为：{优先级 or 当前时间毫秒数} + random(最小停顿间隔, 最大停顿间隔)
    @Column
    private Integer minHalt; // 最小停顿间隔，毫秒
    @Column
    private Integer maxHalt; // 最大停顿间隔，毫秒
    @Column
    private String params; // 启动参数
    @Column
    private String content; // 发布的配置快照内容，用于任务的JSON序列化缓存
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
