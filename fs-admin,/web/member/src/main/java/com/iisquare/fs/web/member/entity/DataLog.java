package com.iisquare.fs.web.member.entity;

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
public class DataLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String permits; // 请求的数据权限，以逗号分割
    @Column
    private String serverId; // 服务端标识，用于区分应用服务，可以是内部的服务名称
    @Column
    private String clientId; // 客户端标识，用于区分调用者，可以是用户ID或者授权的第三方应用
    @Column
    private String requestUrl; // 服务端地址
    @Column
    private String requestIp; // 调用端原始地址
    @Column
    private String requestHeaders; // 调用端原始请求头
    @Column
    private String requestParams; // 调用端原始请求参数
    @Column
    private Long requestTime; // 请求时间

}
