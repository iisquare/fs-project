package com.iisquare.fs.web.kg.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * 图数据库结构与本体的关联登记
 *
 * Neo4j 自身无法标记一条索引或约束来自何处，通过该表登记结构来源，支持结构对账与漂移巡检。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class SchemaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 索引或约束名称，图数据库中唯一
    @Column
    private String kind; // INDEX-索引，CONSTRAINT-约束
    @Column
    private String subType; // 索引类型或约束类型
    @Column
    private Integer ontologyId; // 来源本体，0表示手工创建
    @Column
    private String ontologyType; // NODE-节点，REL-关系
    @Column
    private String label; // 标签或关系类型
    @Column
    private String fields; // 字段列表，以英文逗号分割
    @Column
    private String propertyType; // 属性类型约束的类型
    @Column
    private String definition; // 结构定义JSON
    @Column
    private String createStatement; // 创建语句
    @Column
    private Integer status; // 1-已应用，2-已失效
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
