package com.iisquare.fs.web.lm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户积分（开通用户）
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Credit implements Serializable {

    @Id
    private Integer uid;
    @Column
    private BigDecimal remained; // 剩余
    @Column
    private BigDecimal consumed; // 总消耗
    @Column
    private String rateIds; // 速率限制
    @Column
    private Integer remindEnabled; // 启用余额预警
    @Column
    private Double remindThreshold; // 预警阈值
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
