package com.iisquare.fs.web.govern.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
/**
 * 元模型与模型之间的引用关系
 */
public class ModelRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String sourceCatalog; // 源路径
    @Column
    private String sourceModel; // 源模型
    @Column
    private String sourceColumn; // 源属性字段，为空时标识为类表
    /**
     * 关系类型
     *  inherit-继承（含父子类继承泛化，接口实现）
     *  depend-依赖
     *  association-关联
     *  aggregation-聚合
     *  composition-组合
     *  foreign-外键，仅用于属性字段
     */
    @Column
    private String relation;
    @Column
    private String targetCatalog; // 目标路径
    @Column
    private String targetModel; // 目标模型
    @Column
    private String targetColumn; // 目标属性字段，为空时标识为类表
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
