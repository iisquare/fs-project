package com.iisquare.fs.site.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 行政区划
 * @see(https://xingzhengquhua.bmcx.com/)
 * 省市区坐标边界范围
 * @see(https://github.com/xiangyuecn/AreaCity-JsSpider-StatsGov)
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class DimRegion implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String code; // 编码（唯一约束）
    @Column
    private String name; // 简称
    @Column
    private String fullName; // 完整省市区
    @Column
    private Integer parentId; // 父级
    @Column
    private Integer level; // 层级
    @Column
    private String letter; // 首字母
    @Column
    private String pinyin; // 拼音
    @Column
    private String longitude; // 经度
    @Column
    private String latitude; // 纬度
    @Column
    private String polygon; // 边界
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
    private String createdIp;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

}
