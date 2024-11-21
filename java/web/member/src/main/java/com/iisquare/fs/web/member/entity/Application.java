package com.iisquare.fs.web.member.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Application implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String serial; // 标识（唯一）
    @Column
    private String name;
    @Column
    private String icon; // 图标
    @Column
    private String url; // 为空时不在前台展示
    @Column
    private String target; // 打开方式
    @Column
    private Integer sort;
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

    public ObjectNode menu() {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", id);
        node.put("name", name);
        node.put("icon", icon);
        node.put("url", url);
        node.put("target", target);
        node.put("description", description);
        node.putArray("children");
        return node;
    }

}
