package com.iisquare.fs.web.agent.entity;

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
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 工具名称
    @Column
    private String type; // 工具类型：schema-自定义，mcp-MCP服务
    @Column
    private String url; // 调用地址
    @Column
    private String header; // 请求头，格式为{ key: value }对象
    @Column
    private String query; // 查询参数，格式为{ key: value }对象
    @Column
    private String labels; // 标签，英文逗号分割
    @Column
    private String roleIds; // 授权角色，留空为不限制
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
