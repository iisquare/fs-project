package com.iisquare.fs.web.member.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
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
public class Menu implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String name;
    @Column
    private String fullName; // 自动拼接上级名称
    @Column
    private Integer applicationId;
    @Column
    private Integer parentId;
    @Column
    private String icon; // 图标
    @Column
    private String url;
    @Column
    private String target; // 打开方式
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

    public ObjectNode menu() {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", id);
        node.put("parentId", parentId);
        node.put("name", name);
        node.put("icon", icon);
        node.put("url", url);
        node.put("target", target);
        node.put("description", description);
        node.putArray("children");
        return node;
    }

}
