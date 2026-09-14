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
 * 知识融合候选：疑似同一实体的两条数据，等待人工确认
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class FusionCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer taskId;
    @Column
    private Integer ruleId;
    @Column
    private Integer ontologyId;
    @Column
    private String entityLabel;
    @Column
    private String leftKey;
    @Column
    private String rightKey;
    @Column
    private Double score;
    @Column
    private String detail; // [{name,left,right,score,weight}]
    @Column
    private String status; // PENDING-待确认，MERGED-已合并，REJECTED-不是同一实体
    @Column
    private Integer uid;
    @Column
    private Long createdTime;
    @Column
    private Long updatedTime;

}
