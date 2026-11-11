package com.iisquare.fs.web.spider.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 代理渠道配置。
 * adapter 描述接入方式，例如 static、generic、kuaidaili、juliang、zhima、ipidea、brightdata。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class ProxyChannel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 渠道名称
    @Column
    private String adapter; // 接入适配器
    @Column
    private String protocol; // 默认代理协议
    @Column(length = 4096)
    private String endpoint; // 提取接口地址
    @Column(columnDefinition = "longtext")
    private String credentials; // JSON，令牌与用户名密码
    @Column(columnDefinition = "longtext")
    private String config; // JSON，提取参数与响应字段映射
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
