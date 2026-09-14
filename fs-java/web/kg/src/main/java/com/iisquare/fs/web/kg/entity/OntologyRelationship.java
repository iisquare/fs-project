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
 * 本体关系定义，对应图数据库中的关系类型
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class OntologyRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer ontologyId;
    @Column
    private String code;
    @Column
    private String name;
    @Column
    private String label; // 关系类型
    @Column
    private String description;
    @Column
    private Integer sourceEntityId;
    @Column
    private Integer targetEntityId;
    @Column
    private String mergeFields; // 关系键字段，以英文逗号分割
    @Column
    private Integer cascadeDelete; // 删除实体时是否级联删除该关系
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
