package com.iisquare.fs.web.cron.entity;

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
public class Flow implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 项目名称
    @Column
    private String name; // 流程名称
    @Column
    private String expression; // Cron表达式
    @Column
    Integer concurrent; // 并发度
    @Column
    private String concurrency; // 并发策略
    @Column
    private String failure; // 失败策略
    @Column
    private String data; // 默认参数
    @Column
    private String notify; // 消息通知
    @Column
    private String content;
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
