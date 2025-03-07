package com.iisquare.fs.web.govern.entity;

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
/**
 * 元模型
 */
@IdClass(Model.IdClass.class)
public class Model implements Serializable {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private String catalog;
        private String code;
    }

    @Id
    private String catalog; // 所属包路径，以/分割和结尾，顶级包为/
    @Id
    private String code; // 编码，包、类/表
    @Column
    private String name; // 名称
    @Column
    private String type; // 类型[catalog-包，table-类（含接口、抽象类）]
    @Column
    private String pk; // 模型主键，以英文逗号分割
    @Column
    private Integer sort;
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

    public String path() {
        if ("catalog".equals(type)) {
            return catalog + code + "/";
        } else {
            return catalog + code;
        }
    }

    public static boolean safeCatalog(String catalog) {
        if (DPUtil.empty(catalog)) return false;
        return catalog.matches("^/([a-zA-Z0-9.\\-_]+/)*$");
    }

    public boolean safeCatalog() {
        return safeCatalog(catalog);
    }

    public static boolean safeCode(String code) {
        if (DPUtil.empty(code)) return true;
        return code.matches("^[a-zA-Z0-9.\\-_]+$");
    }

    public boolean safeCode() {
        return safeCode(code);
    }

}
