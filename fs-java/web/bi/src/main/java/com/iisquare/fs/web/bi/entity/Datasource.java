package com.iisquare.fs.web.bi.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Datasource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true)
    private String name;
    @Column
    private String type; // 数据源类型
    @Column
    private Integer olapable; // 是否OLAP可用，1-是，0-否
    @Column
    private String content; // 数据源配置，JSON格式，配置项参考datasource.XXXConnector
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Column
    private String description;
    @Column
    private Integer createdUid;
    @Column
    private Long createdTime;
    @Column
    private Integer updatedUid;
    @Column
    private Long updatedTime;

}
