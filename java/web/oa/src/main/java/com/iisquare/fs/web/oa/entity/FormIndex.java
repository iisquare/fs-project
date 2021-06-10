package com.iisquare.fs.web.oa.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class FormIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private Integer frameId; // 所属表单
    @Column
    private Integer dataId; // 所属数据
    @Column
    private String path; // 字段（以“.”点连接）
    @Column
    private String content; // 值

}
