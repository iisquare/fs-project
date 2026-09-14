package com.iisquare.fs.web.member.entity;

import lombok.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * 用户角色关联，关系维护在独立关联表中
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@IdClass(UserRole.IdClass.class)
public class UserRole implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private Integer userId; // 用户主键
        private Integer roleId; // 角色主键
    }

    @Id
    private Integer userId;
    @Id
    private Integer roleId;
    @Column
    private Long createdTime; // 授权时间
    @Column
    private Integer createdUid; // 授权人

}
