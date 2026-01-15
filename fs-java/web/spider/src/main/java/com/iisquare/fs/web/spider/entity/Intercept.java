package com.iisquare.fs.web.spider.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 定向采集拦截器配置
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Intercept implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer templateId; // 所属模板标识
    @Column
    private Integer code; // 拦截页面响应状态码
    @Column
    private String name; // 拦截器名称
    @Column
    private String charset; // 页面编码，默认与Page的编码一致
    @Column
    private String collection; // 采集结果存储到Mongo中的集合名称
    @Column
    private String parser; // 页面解析器
    /**
     * 辅助器
     * context: { // 上下文
     *     url: '', // 实际请求的链接地址
     *     job: {}, // 作业信息
     *     task: {}, // 任务信息
     *     body: '', // 请求结果
     *     parsed: {}, // 解析结果
     *     status: 200, // 请求状态
     *     exception: null, // 异常信息
     * }
     * return: { // 返回结果，若为空则不启用助手
     *     name: '', // 助手名称
     *     args: {}, // 传递参数
     * }
     */
    @Column
    private String assistant; // 辅助器
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
     *     next: '', // 下步动作，retry - 重试任务，halt - 停顿令牌并重试，discard - 丢弃任务
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
