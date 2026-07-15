package com.iisquare.fs.web.lm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Knowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 知识库名称
    @Column
    private String embeddingModel; // 词嵌入模型
    @Column
    private String rerankModel; // 重排序模型
    @Column(name = "top_k")
    private Integer topK; // 召回数量
    @Column
    private Double score; // 召回阈值
    @Column
    private String recallType; // 召回方式，vector-向量、text-全文、hybrid-混合
    @Column
    private Integer reranked; // 是否启用重排序
    @Column
    private String recallScope; // 召回范围，chunk-检索块、segment-父子分段、document-全文
    @Column
    private String splitSeparator; // 段落分隔符
    @Column
    private Integer splitSegmentTokens; // 分段长度
    @Column
    private Integer splitChunkTokens; // 分块长度
    @Column
    private Integer splitOverlayTokens; // 重叠长度
    @Column
    private String labels; // 标签，英文逗号分割
    @Column
    private String roleIds; // 授权角色，留空为不限制
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Column
    private String description;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
