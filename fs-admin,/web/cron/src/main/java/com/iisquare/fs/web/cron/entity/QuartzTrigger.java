package com.iisquare.fs.web.cron.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
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
@IdClass(QuartzTrigger.IdClass.class)
@Table(name="QRTZ_TRIGGERS")
public class QuartzTrigger implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private String schedule;
        private String name;
        private String group;
    }

    @Id
    @Column(name="SCHED_NAME")
    private String schedule;
    @Id
    @Column(name="TRIGGER_NAME")
    private String name;
    @Id
    @Column(name="TRIGGER_GROUP")
    private String group;
    @Column(name="JOB_NAME")
    private String jobName;
    @Column(name="JOB_GROUP")
    private String jobGroup;
    @Column(name="DESCRIPTION")
    private String description;

    @Column(name="PREV_FIRE_TIME")
    private Long previousFireTime;
    @Column(name="PRIORITY")
    private Integer priority;
    @Column(name="TRIGGER_STATE")
    private String state;
    @Column(name="TRIGGER_TYPE")
    private String type;
    @Column(name="START_TIME")
    private Long startTime;
    @Column(name="END_TIME")
    private Long endTime;
    @Column(name="CALENDAR_NAME")
    private Long calendar;
    @Column(name="MISFIRE_INSTR")
    private Integer misfire;

    @Lob
    @Column(name="JOB_DATA")
    private Blob data;
    @Transient
    private String arg;

    @Transient
    private Date nextFireTime;
    @Transient
    private Date finalFireTime;
    @Transient
    private String expression = "";

}
