package com.iisquare.fs.site.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 行业分类
 * @see(https://github.com/Hyhyhyhyhyhyh/CN_industry_code_2017)
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class DimIndustry implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String serial; // 账号（唯一约束）
    @Column
    private String name; // 昵称（唯一约束）
    @Column
    private String password;
    @Column
    private String email;
    @Column
    private String phone;
    @Column
    private String salt; // 密码盐
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
    private Long loginTime; // 最后登录时间
    @Column
    private String loginIp; // 最后登录IP
    @Column
    private Long lockedTime; // 锁定时间
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

}
