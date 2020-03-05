package com.iisquare.fs.web.member.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "etl_relation")
public class Relation implements Serializable {

    @Id
    private String id;
    @Column
    private String type;
    @Column
    private Integer aid;
    @Column
    private Integer bid;

}
