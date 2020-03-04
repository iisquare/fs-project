package com.iisquare.fs.web.admin.entity;

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
@Table(name = "etl_not_exists")
@IdClass(IdDemo.IdClass.class)
/**
 * 联合主键示例
 */
public class IdDemo implements Serializable {

    @Data
    public class IdClass implements Serializable {
        private Integer aid;
        private String bid;
    }

    @Id
    private Integer aid;
    @Id
    private String bid;
    @Column
    private String other;

}
