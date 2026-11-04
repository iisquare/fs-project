package com.iisquare.fs.web.oa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
public class PrintDocument {

    @Id
    private Integer printId;
    @Lob
    @Column(nullable = false)
    private byte[] templateData;
    @Column
    private Long updatedTime;
    @Column
    private Integer updatedUid;

}
