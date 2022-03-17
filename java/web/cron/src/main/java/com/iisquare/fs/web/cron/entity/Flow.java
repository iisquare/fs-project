package com.iisquare.fs.web.cron.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
@IdClass(Flow.IdClass.class)
public class Flow implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private String project;
        private String name;
    }

    @Id
    private String project; // 项目名称
    @Id
    private String name; // 流程名称
    @Column
    private String expression; // Cron表达式
    @Column
    private String data; // 默认参数
    @Column
    private String notify; // 消息通知
    @Column
    private String content;
    @Column
    private Integer sort;
    @Column
    private String description;
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

    @Transient
    private String state;
    @Transient
    private Date startTime;
    @Transient
    private Date endTime;
    @Transient
    private Date previousFireTime;
    @Transient
    private Date nextFireTime;
    @Transient
    private Date finalFireTime;

}
