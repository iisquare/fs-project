package com.iisquare.fs.web.oa.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private String content; // 流程定义
    @Column
    private Integer formId; // 关联表单
    @Transient
    private ObjectNode formInfo;
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Transient
    private String statusText;
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
    @Column
    private String deploymentId; // 流程部署标识
    @Transient
    private ObjectNode deploymentInfo;
    @Column
    private Integer deploymentUid;
    @Transient
    private String deploymentUidName;

}
