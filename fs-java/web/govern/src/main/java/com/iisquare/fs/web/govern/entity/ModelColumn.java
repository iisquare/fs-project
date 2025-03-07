package com.iisquare.fs.web.govern.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
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
 * 元模型属性或字段
 */
@IdClass(ModelColumn.IdClass.class)
public class ModelColumn implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private String catalog; // 包
        private String model; // 表
        private String code; // 字段
    }

    @Id
    private String catalog; // 所属包路径，以/分割
    @Id
    private String model; // 编码，包、类/表
    @Id
    private String code; // 属性字段，对应meta.name属性
    @Column
    private String name; // 名称
    @Column
    private String type; // 字段类型
    @Column
    private Integer size; // 字段长度
    @Column
    private Integer digit; // 字段小数位数
    @Column
    private Integer nullable; // 是否允许为空
    @Column
    private Integer sort; // 字段位置排序，从1开始，对应meta.position属性
    @Column
    private String description; // 字段描述，对应meta.remark属性

    public String path() {
        return catalog + model + "/" + code;
    }

}
