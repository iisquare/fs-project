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
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 智能体名称
    @Column
    private String model; // 模型名称
    @Column
    private String token; // 认证标识
    @Column
    private String systemPrompt; // 系统提示词
    @Column
    private Integer maxTokens; // 要生成的最大令牌数，为0不传递该参数
    @Column
    private Float temperature; // 多样性
    @Column
    private String parameter; // 自定义参数，JSON格式
    @Column
    private String roleIds; // 授权角色，以逗号分割
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
