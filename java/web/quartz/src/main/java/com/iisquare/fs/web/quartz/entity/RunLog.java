package com.iisquare.fs.web.quartz.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="QRTZ_RUN_LOG")
@DynamicInsert
@DynamicUpdate
@Data
public class RunLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name="TAG")
    private String tag;
    @Column(name="JOB_NAME")
    private String name;
    @Column(name="JOB_GROUP")
    private String group;
    @Column(name="JOB_CLASS_NAME")
    private String jobClass;
    @Column(name="PARAMS")
    private String params;
    @Column(name="NODE")
    private String node;
    @Column(name="TYPE")
    private Integer type;
    @Column(name="RESULT")
    private Integer result;
    @Column(name="ORI_RESULT")
    private String oriResult;
    @Column(name="CREATE_TIME")
    private Long ctime;
    @Transient
    private String url = "";
}
