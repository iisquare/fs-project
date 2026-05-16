package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.DataPermitLog;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface DataPermitLogDao extends DaoBase<DataPermitLog, Long> {

    @Modifying
    @Transactional
    @Query("delete from DataPermitLog where logId in (:ids)")
    Integer deleteByLogIds(@Param("ids") Collection<Long> ids);

}
