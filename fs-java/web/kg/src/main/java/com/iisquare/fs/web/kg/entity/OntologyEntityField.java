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
 * 本体实体的属性定义
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class OntologyEntityField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer entityId;
    @Column
    private String name; // 属性名
    @Column
    private String title; // 显示名称
    @Column
    private String type; // 数据类型
    @Column
    private Integer requiredFlag; // 保存数据时是否必填
    @Column
    private Integer displayFlag; // 是否在画布中展示
    @Column
    private String comment;
    @Column
    private Integer sort;

}
