package com.iisquare.fs.web.member.entity;

import lombok.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * 角色资源关联，资源所属应用由资源实体推导，关系维护在独立关联表中
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@IdClass(RoleResource.IdClass.class)
public class RoleResource implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private Integer roleId; // 角色主键
        private Integer resourceId; // 资源主键
    }

    @Id
    private Integer roleId;
    @Id
    private Integer resourceId;
    @Column
    private Long createdTime; // 授权时间
    @Column
    private Integer createdUid; // 授权人

}
