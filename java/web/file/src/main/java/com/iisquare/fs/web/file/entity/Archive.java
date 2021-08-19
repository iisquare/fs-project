package com.iisquare.fs.web.file.entity;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
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
    private String bucket; // 存储分类
    @Column
    private String dir; // 存储目录
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
    @Transient
    private String statusText;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Transient
    private String createdUidName;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;
    @Transient
    private String updatedUidName;

    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static String dir(String uuid) {
        return DPUtil.substring(uuid, -2, 2) + "/" + DPUtil.substring(uuid, -4, 2);
    }

    public String path() {
        return dir + "/" + id + suffix;
    }

    public static String suffix(String filename) {
        String[] strings = DPUtil.explode(filename, "\\.");
        if (strings.length < 2) return "";
        String suffix = strings[strings.length - 1];
        return DPUtil.isMatcher("^[a-zA-Z\\d]+$", suffix) ? "." + suffix : "";
    }

}
