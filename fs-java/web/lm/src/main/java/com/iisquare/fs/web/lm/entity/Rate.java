package com.iisquare.fs.web.lm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 请求速率限制
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Rate implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 名称
    @Column
    private Double requestCount; // 间隔内请求数量
    @Column
    private Integer requestInterval; // 请求间隔
    @Column
    private Double tokenCount; // 间隔内词元数量
    @Column
    private Integer tokenInterval; // 词元间隔
    @Column
    private Double creditCount; // 间隔内积分数量
    @Column
    private Integer creditInterval; // 积分间隔
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Column
    private String description;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
