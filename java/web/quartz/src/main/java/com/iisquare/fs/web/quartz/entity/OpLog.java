package com.iisquare.fs.web.quartz.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="QRTZ_OP_LOG")
@DynamicInsert
@DynamicUpdate
@Data
public class OpLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name="JOB_NAME")
    private String name;
    @Column(name="JOB_GROUP")
    private String group;
    @Column(name="TYPE")
    private Integer type;
    @Column(name="PARAMS")
    private String params;
    @Column(name="CREATE_UID")
    private Integer cuid;
    @Column(name="CREATE_TIME")
    private Long ctime;
}
