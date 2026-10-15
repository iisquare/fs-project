package com.iisquare.fs.web.bi.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 计算集群配置：远程或本地 Spark / Flink 集群
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Cluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true)
    private String name;
    @Column
    private String engine; // 计算引擎：spark / flink
    @Column
    private String mode; // 运行模式：local / remote
    @Column
    private String content; // 集群连接与提交配置，JSON格式
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Transient
    private String statusText;
    @Transient
    private String engineText;
    @Transient
    private String modeText;
    @Transient
    private String summary;
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
