package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.Data;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataDao extends DaoBase<Data, Integer> {

    @Query(value = "select count(*) from Data where serial = :serial and id != :id")
    int exist(@Param("serial") String serial, @Param("id") int id);

}
