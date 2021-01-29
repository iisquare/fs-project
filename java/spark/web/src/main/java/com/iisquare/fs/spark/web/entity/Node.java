package com.iisquare.fs.spark.web.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Node implements Serializable {

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
    @Transient
    private List<Node> children;
    @Column
    private String type; // 分类
    @Column
    private String plugin; // 从属插件
    @Column
    private String icon; // 图标
    @Column
    private String state; // 展开状态
    @Column
    private String classname; // 类名称
    @Column
    private Integer draggable; // 是否可拖拽
    @Column
    private String properties; // 属性JSON配置
    @Column
    private String returns; // 返回值JSON配置
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
