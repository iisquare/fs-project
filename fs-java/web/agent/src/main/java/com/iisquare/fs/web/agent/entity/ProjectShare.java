package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 分享的项目：客户端发布上来的项目配置按「项目 + 版本」两级组织。
 *
 * 基本信息（名称、图标、描述）随每次发布同步，项目配置本身挂在版本上（见 {@link ProjectVersion}）；
 * 授权角色留空表示所有登录用户都看得到，配置后要求调用人的角色与之有交集（与技能同一套口径）。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class ProjectShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 项目名称
    @Column
    private String icon; // 图标地址
    @Column
    private String roleIds; // 授权角色，留空为所有登录用户可见
    @Column
    private Integer sort;
    @Column
    private Integer status; // 1-启用，2-停用（停用后客户端不再可见）
    @Column
    private String description;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid; // 分享人：只有本人可以继续追加版本
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
