package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.DataPermit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface DataPermitDao extends DaoBase<DataPermit, Integer> {

    @Modifying
    @Transactional
    @Query("delete from DataPermit where dataId=:ids")
    Integer deleteByDataIds(@Param("ids") Collection<Integer> ids);

}
