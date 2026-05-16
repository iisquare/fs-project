package com.iisquare.fs.web.govern.entity;

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
/**
 * 数据质量检测规则
 */
public class QualityRule implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String type; // 规则类型
    @Column
    private String name; // 规则名称
    @Column
    private Integer logicId; // 所属分类
    @Transient
    private String logicIdName; // 所属分类名称
    @Column
    private String checkTable; // 检查表名
    @Column
    private String checkColumn; // 检查字段
    @Column
    private String checkMetric; // 检查字段的聚合方式
    @Column
    private String checkWhere; // 前置过滤条件
    @Column
    private String checkGroup; // 分组字段，英文逗号分割
    @Column
    private String referTable; // 引用表名
    @Column
    private String referColumn; // 引用字段
    @Column
    private String referMetric; // 引用字段的聚合方式
    @Column
    private String referWhere; // 引用表的前置过滤条件
    @Column
    private String referGroup; // 引用表的分组字段，英文逗号分割
    @Column
    private String content; // 规则参数
    @Column
    private String suggest; // 整改建议
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Transient
    private String statusText;
    @Column
    private String description;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Transient
    private String createdUidName;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;
    @Transient
    private String updatedUidName;

}
