package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * agent 外部调用密钥。
 *
 * secret 与 LM 授权密钥一致，保存密钥原文供授权管理员查看和复制。
 * agenticIds 是流程白名单；请求携带有效 x-auth-token 时始终按会话用户记账。
 */
@Entity
@Table(name = "api_key")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class AgentApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private Integer uid; // 密钥所属用户
    @Column
    private String secret; // 密钥
    @Column
    private String agenticIds; // 允许调用的编排流程
    @Column
    private Integer status; // 1-启用，2-禁用
    @Column
    private Long expiredTime;
    @Column
    private Long lastUsedTime;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

}
