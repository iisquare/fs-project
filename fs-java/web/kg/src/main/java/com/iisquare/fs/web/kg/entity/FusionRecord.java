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
 * 知识融合记录：合并结果留痕，不提供撤销
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class FusionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer candidateId;
    @Column
    private Integer ontologyId;
    @Column
    private String entityLabel;
    @Column
    private String keepKey;
    @Column
    private String mergedKey;
    @Column
    private String keepSnapshot;
    @Column
    private String mergedSnapshot;
    @Column
    private Integer relationsMoved;
    @Column
    private Integer relationsMerged;
    @Column
    private Integer relationsRemoved;
    @Column
    private Integer labelsMerged;
    @Column
    private Integer uid;
    @Column
    private Long createdTime;

}
