package com.iisquare.fs.web.bi.entity;

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
public class DataExcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true)
    private String name;
    @Column
    private String pks; // 主键，以逗号分割
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
