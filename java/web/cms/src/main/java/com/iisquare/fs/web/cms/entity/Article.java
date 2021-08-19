package com.iisquare.fs.web.cms.entity;

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
public class Article implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String title;
    @Column
    private Integer catalogId;
    @Transient
    private String catalogIdName;
    @Column
    private String cover; // 封面，图库标识
    @Column
    private String keyword; // 关键词
    @Column
    private String label; // 系统标签，英文逗号分割
    @Column
    private String tag; // 自定义标签，英文逗号分割
    @Column
    private String citeName; // 引用标识，默认为本站
    @Column
    private String citeAuthor; // 引用作者，默认为修改者
    @Column
    private String citeUrl; // 引用地址，默认为本站地址
    @Column
    private String password; // 阅读密码
    @Column
    private String format; // 文章格式
    @Column
    private String content; // 文章内容
    @Column
    private String html; // 展示效果
    @Column
    private Integer countView; // 浏览量
    @Column
    private Integer countApprove; // 赞成数
    @Column
    private Integer countOppose; // 反对数
    @Column
    private Integer countComment; // 评论数
    @Column
    private Integer sort; // 逆序
    @Column
    private Integer status;
    @Transient
    private String statusText;
    @Column
    private String description; // 描述
    @Column
    private Long publishTime; // 发布时间逆序，可以编辑的展示项
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
