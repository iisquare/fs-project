package com.iisquare.fs.web.bi.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

/**
 * 数据主题，组合多个数据集并配置字段之间的关联关系。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class DataTheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true)
    private String name;
    /**
     * 主题配置，JSON格式：
     * {
     *   "datasetIds": [1, 2],
     *   "relations": [{
     *     "id": "关联关系标识",
     *     "sourceDatasetId": 1,
     *     "targetDatasetId": 2,
     *     "sourceFields": ["user_id", "tenant_id"],
     *     "targetFields": ["id", "tenant_id"],
     *     "description": "外键说明"
     *   }]
     * }
     */
    @Column
    private String content;
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
