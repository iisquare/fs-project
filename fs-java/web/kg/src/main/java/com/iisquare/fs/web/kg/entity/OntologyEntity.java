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
 * 本体实体定义，对应图数据库中的节点标签
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class OntologyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer ontologyId;
    @Column
    private String code; // 编码
    @Column
    private String name; // 显示名称
    @Column
    private String label; // 节点标签
    @Column
    private String description;
    @Column
    private String icon;
    @Column
    private String color;
    @Column
    private String primaryField; // 主键字段
    @Column
    private String captionField; // 标题字段
    @Column
    private Integer extendable; // 是否允许保存本体未声明的属性，0-否，1-是
    @Column
    private Integer extendableLabels; // 是否允许数据扩展标签，0-否，1-是
    @Column
    private Integer sort;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
