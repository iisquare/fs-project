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
import org.hibernate.annotations.DynamicUpdate;

/**
 * 本地客户端的一个安装包版本。
 *
 * 每条记录只属于一个系统 / 架构，version 是安装包版本，minimumVersion 是仍允许运行的最低版本：
 * 客户端低于它会强制升级；latest 只按同一系统 / 架构下的最高启用版本计算。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class ClientVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String platform; // win32 / darwin / linux
    @Column
    private String architecture; // x64 / arm64 / universal
    @Column
    private String channel; // stable / beta
    @Column
    private String version;
    @Column
    private String minimumVersion;
    @Column
    private String releaseNotes;
    @Column
    private String fileId;
    @Column
    private String filepath;
    @Column
    private String fileName;
    @Column
    private Long fileSize;
    @Column
    private String sha512;
    @Column
    private String deltaManifest; // 固定块差分清单（JSON）
    @Column
    private Integer rolloutPercentage; // 灰度发布比例 0-100
    @Column
    private Integer allowRollback; // 1 表示允许从更高版本降级到本版本
    @Column
    private Integer status; // 1-启用，2-停用
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
