package com.iisquare.fs.web.cron.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Blob;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
@IdClass(QuartzJob.IdClass.class)
@Table(name="QRTZ_JOB_DETAILS")
public class QuartzJob implements Serializable {

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
    @Column(name="JOB_NAME")
    private String name;
    @Id
    @Column(name="JOB_GROUP")
    private String group;
    @Column(name="DESCRIPTION")
    private String description;
    @Column(name="JOB_CLASS_NAME")
    private String cls;

    @Column(name="IS_DURABLE")
    private String durable;
    @Column(name="IS_NONCONCURRENT")
    private String nonConcurrent;
    @Column(name="IS_UPDATE_DATA")
    private String updateData;
    @Column(name="REQUESTS_RECOVERY")
    private String recovery;

    @Lob
    @Column(name="JOB_DATA")
    private Blob data;
    @Transient
    private String arg;

}
