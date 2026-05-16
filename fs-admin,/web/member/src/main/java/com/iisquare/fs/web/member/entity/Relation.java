package com.iisquare.fs.web.member.entity;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Relation implements Serializable {

    @Id
    private String id; // type_aid_bid_cid
    @Column
    private String type; // 关联业务，aid所属业务_cid所属业务
    @Column
    private Integer aid; // 业务主键
    @Column
    private Integer bid; // 业务主键
    @Column
    private Integer cid; // 同aid和bid建立联系的中间业务主键，若不存在可不设置

}
