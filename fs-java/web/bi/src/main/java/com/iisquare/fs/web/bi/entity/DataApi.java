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
public class DataApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true)
    private String name;
    @Column
    private String url; // 接口地址
    @Column
    private String method; // 请求方式
    @Column
    private Integer timeout; // 请求超时
    @Column
    private String headers; // 请求头，JSON格式：{ key: value }
    @Column
    private String contentType; // 请求体类型
    @Column
    private String payloadForm; // form-data、x-www-from-urlencoded
    @Column
    private String payloadBody; // json、xml、raw
    @Column
    private String pks; // 主键，以逗号分割
    @Column
    private String pageRequestField; // 页码传参字段
    @Column
    private String pageSizeRequestField; // 分页大小传参字段
    @Column
    private String pageResponseField; // 页码返回字段
    @Column
    private String pageSizeResponseField; // 分页大小返回字段
    @Column
    private String totalResponseField; // 记录总数返回字段
    /**
     * 字段配置，JSON格式：[{
     *     "path": "请求结果的字段路径，数组或对象的下级使用.点分割",
     *     "field": "请求结果的字段名称",
     *     "name": "数据字段名称",
     *     "title": "数据字段标题，为空时以name为准",
     *     "type": "数据类型，参考DatasetService.fieldTypes()",
     *     "comment": "字段备注",
     *     "checked": false, // 是否应用
     *     "children": []
     * }]
     *
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
