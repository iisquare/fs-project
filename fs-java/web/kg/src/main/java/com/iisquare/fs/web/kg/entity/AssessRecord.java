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
 * 知识评估记录：按本体定义评估图数据质量
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class AssessRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer ontologyId;
    @Column
    private String ontologyName;
    @Column
    private Double score;
    @Column
    private Integer nodeCount;
    @Column
    private Integer relationshipCount;
    @Column
    private Integer issueCount;
    @Column
    private String detail;
    @Column
    private Integer uid;
    @Column
    private Long createdTime;

}
