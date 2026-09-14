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
 * 数据检索方案，保存常用检索条件便于复用
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class DataQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer ontologyId;
    @Column
    private String kind; // ENTITY-实体，RELATIONSHIP-关系
    @Column
    private String label; // 标签或关系类型
    @Column
    private String name; // 方案名称
    @Column
    private String params; // 检索条件JSON
    @Column
    private Integer uid;
    @Column
    private Long createdTime;
    @Column
    private Long updatedTime;

}
