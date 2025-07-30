package com.iisquare.fs.web.face.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Photo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private Integer userId;
    @Transient
    private User userIdInfo;
    @Column
    private Integer sort;
    @Column
    private Integer cover; // 是否作为封面
    @Column
    private String base64; // 图像文件，授权访问，防止泄露
    @Column
    private String face; // 面部区域图像
    @Column
    private String box; // 矩形区域
    @Column
    private String square; // 正方形区域
    @Column
    private String landmark; // 人脸关键点
    @Column
    private String eigenvalue; // 特征值
    @Column
    private Integer status;
    @Transient
    private String statusText;
    @Column
    private String description;
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

    public Photo(Integer userId, String eigenvalue) {
        this.userId = userId;
        this.eigenvalue = eigenvalue;
    }

    public Photo(Integer id, Integer userId, String face) {
        this.id = id;
        this.userId = userId;
        this.face = face;
    }

}
