package com.iisquare.fs.web.file.entity;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Archive {

    @Id
    private String id;
    @Column
    private String name; // 文件名称
    @Column
    private String bucket; // 存储桶
    @Column
    private String filepath; // 存储路径
    @Column
    private String suffix; // 文件后缀
    @Column
    private String type; // 文件类型
    @Column
    private Long size; // 文件大小
    @Column
    private String digest; // 头部摘要
    @Column
    private String hash; // MD5校验值
    @Column
    private Integer status;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static String suffix(String filename) {
        String[] strings = DPUtil.explode("\\.", filename);
        if (strings.length < 2) return "";
        String suffix = strings[strings.length - 1];
        return DPUtil.isMatcher("^[a-zA-Z\\d]+$", suffix) ? "." + suffix : "";
    }

}
