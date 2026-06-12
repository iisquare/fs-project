package com.iisquare.fs.web.demo.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * 联合主键
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
@IdClass(Composite.IdClass.class)
public class Composite implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private Integer aid;
        private Integer bid;
    }

    @Id
    private Integer aid; // 主键
    @Id
    private Integer bid; // 主键
    @Column
    private String name; // 名称
    @Column
    private Long createdTime; // 创建时间
    @Column
    private Long updatedTime; // 更新时间

    /**
     * 默认由@Builder生成，不需要手动创建
     * 生成的build()方法内部，通过new Composite(...)创建新对象
     */
    public static class CompositeBuilder {

        /**
         * 默认只生成name(String name)的setter方法
         * 若要通过builder获取赋值内容，可通过自定义getter方法实现
         */
        public String getName() {
            return name;
        }

    }

}
