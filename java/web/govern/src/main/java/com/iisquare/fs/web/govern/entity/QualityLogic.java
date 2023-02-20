package com.iisquare.fs.web.govern.entity;

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
/**
 * 数据质量检测分类
 */
public class QualityLogic implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private Integer parentId;
    @Transient
    private String parentIdName;
    @Transient
    private List<QualityLogic> children;
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
