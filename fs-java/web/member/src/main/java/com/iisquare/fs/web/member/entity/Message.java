package com.iisquare.fs.web.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Message implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String type; // 消息类型
    @Column
    private String recipient; // 收件人
    @Column
    private String subject; // 主题
    @Column
    private String requestBody; // 请求内容
    @Column
    private String responseBody; // 返回结果或异常信息
    @Column
    private String status; // 状态
    @Column
    private Long createdTime;
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

}
