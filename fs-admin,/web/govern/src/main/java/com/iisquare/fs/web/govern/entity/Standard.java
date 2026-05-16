package com.iisquare.fs.web.govern.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
/**
 * 数据标准
 */
@IdClass(Standard.IdClass.class)
public class Standard implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private String catalog; // 目录
        private String code; // 编码
    }

    @Id
    private String catalog;
    @Id
    private String code;
    @Column
    private String path; // 完整路径，catalog + code
    @Column
    private String mold; // 记录类型
    @Column
    private String another; // 别名，英文逗号分割
    @Column
    private String name; // 名称
    @Column
    private String flag; // 标志，unify - 统一别名编码
    @Column
    private String level; // 预警等级
    @Column
    private String type; // 字段类型
    @Column
    private Integer size; // 字段长度
    @Column
    private Integer digit; // 字段小数位数
    @Column
    private Integer nullable; // 是否允许为空
    @Column
    private Integer sort; // 排序
    @Column
    private Integer status;
    @Transient
    private String statusText;
    @Column
    private String description; // 备注
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
