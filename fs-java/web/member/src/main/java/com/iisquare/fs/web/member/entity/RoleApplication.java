package com.iisquare.fs.web.member.entity;

import lombok.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * 角色应用关联，关系维护在独立关联表中
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@IdClass(RoleApplication.IdClass.class)
public class RoleApplication implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private Integer roleId; // 角色主键
        private Integer applicationId; // 应用主键
    }

    @Id
    private Integer roleId;
    @Id
    private Integer applicationId;
    @Column
    private Long createdTime; // 授权时间
    @Column
    private Integer createdUid; // 授权人

}
