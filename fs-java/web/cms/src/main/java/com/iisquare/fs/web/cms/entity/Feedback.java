package com.iisquare.fs.web.cms.entity;

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
public class Feedback implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String referType; // 引用类型
    @Column
    private String referId; // 引用标识
    @Transient
    private String referIdDigest; // 引用摘要
    @Column
    private String content; // 反馈内容
    @Column
    private String url; // 来源页面
    @Column
    private String ua; // 浏览器标识
    @Column
    private String ip; // 客户端IP地址
    @Column
    private Integer status;
    @Transient
    private String statusText;
    @Column
    private Long publishTime; // 发布时间
    @Column
    private Integer publishUid;
    @Transient
    private String publishUidName;
    @Column
    private String auditTag; // 审核标签
    @Column
    private String auditReason; // 审核意见
    @Column
    private Long auditTime; // 审核时间
    @Column
    private Integer auditUid;
    @Transient
    private String auditUidName;

}
