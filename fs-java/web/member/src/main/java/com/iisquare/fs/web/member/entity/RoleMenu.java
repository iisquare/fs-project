package com.iisquare.fs.web.member.entity;

import lombok.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * 角色菜单关联，菜单所属应用由菜单实体推导，关系维护在独立关联表中
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@IdClass(RoleMenu.IdClass.class)
public class RoleMenu implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private Integer roleId; // 角色主键
        private Integer menuId; // 菜单主键
    }

    @Id
    private Integer roleId;
    @Id
    private Integer menuId;
    @Column
    private Long createdTime; // 授权时间
    @Column
    private Integer createdUid; // 授权人

}
