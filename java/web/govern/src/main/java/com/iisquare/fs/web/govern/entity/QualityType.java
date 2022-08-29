package com.iisquare.fs.web.govern.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
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
 * 数据质量检测类型
 * 用于配置质量检测规则的执行类和参数
 */
@IdClass(QualityType.IdClass.class)
public class QualityType implements Serializable {

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
    private String mold; // 规则类型：Catalog - 目录，其他 - 类简称
    @Column
    private String name; // 名称
    @Column
    private String content; // 配置内容
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
