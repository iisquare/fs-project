package com.iisquare.fs.web.cron.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.cron.entity.FlowLog;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FlowLogDao extends DaoBase<FlowLog, Integer> {

    @Query("select count(state) from FlowLog where project=:project and name=:name and state=:state")
    int countByState(@Param("project") String project, @Param("name") String name, @Param("state") String state);

    @Query("from FlowLog where state in ('RUNNING')")
    List<FlowLog> canRun();

    @Modifying
    @Transactional
    @Query(value = "update FlowLog set data=:data where id=:id")
    Integer config(@Param("id") Integer id, @Param("data") String data);

    @Modifying
    @Transactional
    @Query(value = "update FlowLog set state=:state, updatedTime=:time where id=:id")
    Integer finish(@Param("id") Integer id, @Param("state") String state, @Param("time") Long time);

}
