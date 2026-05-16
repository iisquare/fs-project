package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.Application;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationDao extends DaoBase<Application, Integer> {

    @Query(value = "select count(*) from Application where serial = :serial and id != :id")
    int exist(@Param("serial") String serial, @Param("id") int id);

}
