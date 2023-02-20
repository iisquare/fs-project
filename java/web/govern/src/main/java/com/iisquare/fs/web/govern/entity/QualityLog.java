package com.iisquare.fs.web.govern.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
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
 * 数据质量检测日志
 */
public class QualityLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String source; // 数据源编码
    @Column
    private Integer planId; // 所属方案
    @Transient
    private String planIdName; // 所属方案名称
    @Column
    private Integer ruleId; // 所属规则
    @Transient
    private String ruleIdName; // 所属规则名称
    @Column
    private Integer logicId; // 所属分类
    @Transient
    private String logicIdName; // 所属分类名称
    @Column
    private String checkTable; // 检查表名
    @Column
    private String checkColumn; // 检查字段
    @Column
    private Integer checkCount; // 检查记录总数
    @Column
    private Integer hitCount; // 命中记录总数
    @Column
    private String reason; // 整改原因
    @Column
    private String expression; // 检查逻辑
    @Column
    private Integer state; // 运行状态码，0正常，非0异常
    @Column
    private Long createdTime;

}
