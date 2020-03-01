package com.iisquare.fs.web.quartz.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.quartz.Trigger;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name="QRTZ_JOB_DETAILS")
@DynamicInsert
@DynamicUpdate
@Data
public class Job implements Serializable {

    @EmbeddedId
    private JobKey key;
    @Column(name="DESCRIPTION")
    private String desc;
    @Column(name="JOB_CLASS_NAME")
    private String jobClass;
    @Transient
    private String cron = "";
    @Transient
    private String params = "{}";
    @Transient
    private Trigger.TriggerState status = Trigger.TriggerState.NONE;
    @Transient
    private Date utime = new Date(0);
}
