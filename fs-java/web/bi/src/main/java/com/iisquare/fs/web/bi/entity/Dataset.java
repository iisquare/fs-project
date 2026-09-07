package com.iisquare.fs.web.bi.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true)
    private String name;
    @Column
    private String type; // 服务方式：direct-直连，cron-定时同步
    @Column
    private String expression; // 定时表达式
    @Column
    private String content; // 查询SQL语句
    @Column
    private String pks; // 主键，以逗号分割
    @Column
    private String partitions; // 分区字段，以逗号分割
    /**
     * 字段配置，JSON格式: [{
     *     "name": "字段名称",
     *     "title": "字段标题，为空时以name为准",
     *     "type": "数据类型，参考DatasetService.fieldTypes()",
     *     "comment": "字段描述"
     * }]
     */
    @Column
    private String fields;
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
    private Long lastSyncedTime; // 最后同步完成时间
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
