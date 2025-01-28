package com.iisquare.fs.web.member.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Dictionary implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name; // 名称，对应选项标签
    @Column
    /**
     * @see(nlp.PinyinJob)
     */
    private String pinyin; // 名称对应的拼音，可用于排序
    @Column
    private Integer ancestorId; // 祖级，为空时标识独立字典，同一字典的祖级应保持一致
    @Column
    private Integer parentId; // 父级，同一字典内排列上下级
    @Column
    private String content; // 内容，对应选项值
    @Column
    /**
     * update fs_project.fs_member_dictionary set leaf = 1 where id not in (
     * 	select parent_id from (
     * 		select count(*) as ct, parent_id  from fs_project.fs_member_dictionary where parent_id != 0 group by parent_id HAVING ct > 0
     * 	) as t
     * ) and ancestor_id != 0
     */
    private Integer leaf; // 是否为叶子节点
    @Column
    private Integer sort;
    @Column
    private Integer status;
    @Column
    private String description;
    @Column
    private Long createdTime;
    @Column
    private Integer createdUid;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
