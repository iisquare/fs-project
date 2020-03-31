package com.iisquare.fs.web.flink.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class AnalysisNode implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private String fullName;
    @Column
    private Integer parentId;
    @Transient
    private String parentIdName;
    @Column
    private String type; // 分类
    @Column
    private String icon; // 图标
    @Column
    private String state; // 展开状态
    @Column
    private Integer draggable; // 是否可拖拽
    @Column
    private String content; // 流程图内容
    @Column
    private String property; // 属性JSON配置
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

}
