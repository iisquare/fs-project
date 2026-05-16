package com.iisquare.fs.web.member.entity;

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
public class DataPermitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private Long logId; // 所属日志标识，同一次鉴权可能包含多个数据授权
    @Column
    private Integer dataId;
    @Column
    private String dataSerial; // 数据模型标识，防止标识变更
    @Column
    private String filters; // 行权限，JSON格式的过滤条件
    @Column
    private String fields; // 列权限，逗号分割的字段列表

}
