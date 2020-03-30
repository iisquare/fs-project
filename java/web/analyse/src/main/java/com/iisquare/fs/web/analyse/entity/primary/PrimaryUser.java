package com.iisquare.fs.web.analyse.entity.primary;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
@Table(name = "test_user")
public class PrimaryUser implements Serializable {

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
    private String salt; // 密码盐

}
