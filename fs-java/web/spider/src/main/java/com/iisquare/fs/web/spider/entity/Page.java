package com.iisquare.fs.web.spider.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 定向采集页面配置
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Page implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer templateId; // 所属模板标识
    @Column
    private String code; // 页面编码
    @Column
    private String name; // 页面名称
    @Column
    private Integer forceMode; // 强制采集
    @Column
    private Long priority; // 页面优先级，非空时替代模板的同名参数
    @Column
    private String url; // 链接地址，支持{变量}占位符
    @Column
    private String charset; // 页面编码，默认utf-8
    @Column
    private String collection; // 采集结果存储到Mongo中的集合名称
    @Column
    private String bucket; // 附件存储到MinIO中的桶名称
    @Column
    private Integer connectTimeout; // 连接超时
    @Column
    private Integer socketTimeout; // 执行超时
    @Column
    private Integer iterateCount; // 翻页深度
    @Column
    private Integer retryCount; // 重试次数
    @Column
    private String headers; // 自定义请求头
    @Column
    private String parser; // 页面解析器
    /**
     * 映射器
     * context: { // 上下文
     *     url: '', // 实际请求的链接地址
     *     job: {}, // 作业信息
     *     task: {}, // 任务信息
     *     body: '', // 请求结果
     *     parsed: {}, // 解析结果
     *     status: 200, // 请求状态
     *     exception: null, // 异常信息
     * }
     * return: { // 返回结果
     *     collect: [{}], // 输出结果
     *     fetch: { // 发起新请求
     *         pageId: [{}], // 页面标识 -> 页面参数
     *     },
     *     iterate: 'url', // 分页迭代链接
     * }
     */
    @Column
    private String mapper;
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
