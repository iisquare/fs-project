package com.iisquare.fs.web.ai.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class LmChatDialog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer chatId;
    @Column
    private Integer parentId;
    @Column
    private String role; // 角色
    @Column
    private String content; // 内容
    @Column
    private String intent; // 意图识别结果
    @Column
    private String reference; // 参考数据
    @Column
    private String finishReason;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long deletedTime;
    @Column
    private Integer deletedUid;

}
