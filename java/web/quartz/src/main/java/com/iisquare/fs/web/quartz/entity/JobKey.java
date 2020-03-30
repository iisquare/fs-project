package com.iisquare.fs.web.quartz.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
public class JobKey implements Serializable {

    @Column(name="SCHED_NAME")
    private String schedName;
    @Column(name="JOB_NAME")
    private String name;
    @Column(name="JOB_GROUP")
    private String group;
}
