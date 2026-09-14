package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.DataLog;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataLogDao extends DaoBase<DataLog, Integer> {

    @Modifying(flushAutomatically = true)
    @Query("delete from DataLog t where t.createdTime < :beforeTime")
    int deleteBefore(@Param("beforeTime") Long beforeTime);

}
