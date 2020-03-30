package com.iisquare.fs.web.demo.entity;

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
@IdClass(Composite.IdClass.class)
/**
 * 联合主键
 */
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

}
