package com.iisquare.fs.web.govern.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
/**
 * 数据标准落地评估日志
 */
public class AssessLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer assess; // 落地评估规则
    @Column
    private String standard; // 标准路径
    @Column
    private String source; // 数据源
    @Column
    private String model; // 表名
    @Column
    private String code; // 字段名称
    @Column
    private String level; // 预警等级
    @Column
    private Integer name = 0; // 字段名不一致
    @Column
    private Integer type = 0; // 字段类型不一致
    @Column
    private Integer size = 0; // 字段长度不一致
    @Column
    private Integer digit = 0; // 字段小数位数不一致
    @Column
    private Integer nullable = 0; // 是否允许为空不一致
    @Column
    private String detail; // 明细
    @Column
    private Long checkTime; // 检测时间

    public boolean different () {
        return name + type + size + digit + nullable > 0;
    }

}
