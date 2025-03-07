package com.iisquare.fs.web.face.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
