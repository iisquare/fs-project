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
import jakarta.persistence.Transient;

/**
 * 图数据变更记录
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class DataLog {

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
    private String action; // SAVE-保存，REMOVE-删除，IMPORT-导入
    @Column
    private String targets; // 操作对象标识
    @Column
    private String payload; // 提交内容
    @Column
    private Integer uid;
    @Transient
    private String uidName;
    @Column
    private Integer resultCode;
    @Column
    private String resultMessage;
    @Column
    private Long createdTime;

}
