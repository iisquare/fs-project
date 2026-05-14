package com.iisquare.fs.web.lm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer providerId; // 所属供应商
    @Column
    private String name; // 模型名称
    @Column
    private String alias; // 模型别名，模型广场展示名称，同名可用于负载均衡，默认为“供应商标识/模型名称”
    @Column
    private String type; // 模型类型
    @Column
    private String roleIds; // 授权角色，留空为不限制
    @Column
    private Integer explorable; // 是否可在模型广场展示
    @Column
    private Integer allVisible; // 无授权用户是否可见
    @Column
    private Integer securityDetectable; // 启用安全围栏
    @Column
    private String plan; // 计费方案（速率限制、上下文长度、计费策略等配置选型）
    @Column
    private String content; // 配置信息
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
