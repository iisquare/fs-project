package com.iisquare.fs.web.face.entity;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
