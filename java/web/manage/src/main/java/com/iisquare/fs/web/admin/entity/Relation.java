package com.iisquare.fs.web.admin.entity;

import lombok.*;

import javax.persistence.*;
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
