package com.iisquare.fs.web.lm.entity;

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
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String title; // 会话标题
    @Column
    private String type; // 会话类型
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;
    @Column
    private String deletedReason; // 删除原因
    @Column
    private String deletedDetail; // 删除描述
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

}
