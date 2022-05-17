package com.iisquare.fs.web.govern.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.govern.entity.AssessLog;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AssessLogDao extends DaoBase<AssessLog, Integer> {

    @Modifying
    @Transactional
    @Query("delete from AssessLog where assess = :assess")
    Integer deleteByAssess(@Param("assess") Integer assess);

}
