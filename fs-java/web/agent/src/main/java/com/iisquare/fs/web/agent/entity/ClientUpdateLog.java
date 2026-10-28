package com.iisquare.fs.web.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

/**
 * 客户端更新结果埋点：下载开始、失败、安装失败以及新版本启动成功。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
public class ClientUpdateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String clientId;
    @Column
    private String platform;
    @Column
    private String architecture;
    @Column
    private String channel;
    @Column
    private String fromVersion;
    @Column
    private String targetVersion;
    @Column
    private String event;
    @Column
    private String message;
    @Column
    private Long createdTime;

}
