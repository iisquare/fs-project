package com.iisquare.fs.web.oa.entity;

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
public class FormData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer frameId; // 所属表单
    @Column
    private String content; // 表单数据
    @Column
    private String bpmInstance; // 流程实例
    @Column
    private String bpmStatus; // 流程状态
    @Column
    private String bpmTask; // 流程当前节点
    @Column
    private String bpmIdentity; // 流程当前负责人
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

}
