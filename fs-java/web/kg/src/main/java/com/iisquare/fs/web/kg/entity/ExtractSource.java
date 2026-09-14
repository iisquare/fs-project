package com.iisquare.fs.web.kg.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * 知识抽取数据源
 *
 * 保存待抽取的文本内容（粘贴文本或从文件解析出的纯文本），是抽取任务的输入。
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class ExtractSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private String type; // TEXT-粘贴文本，FILE-上传文件解析
    @Column
    private String filename;
    @Column
    private Long size; // 字符数
    @Column
    private String content;
    @Column
    private Integer status;
    @Column
    private Integer processStatus; // 处理状态：0-未处理，1-已抽取入图
    @Column
    private Long processTime; // 处理时间
    @Column
    private Integer processUid; // 处理人
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
