package com.iisquare.fs.web.cron.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.cron.entity.FlowStage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface FlowStageDao extends DaoBase<FlowStage, FlowStage.IdClass> {

    @Query("from FlowStage where logId=:logId order by runTime asc")
    List<FlowStage> findAllByLogId(@Param("logId") Integer logId);

    @Query("from FlowStage where logId in (:logIds)")
    List<FlowStage> findAllByLogId(@Param("logIds") Collection<Integer> logIds);

    @Modifying
    @Transactional
    @Query(value = "update FlowStage set runTime=:time, state='RUNNING' where logId=:logId and stageId=:stageId")
    Integer running(@Param("logId") Integer logId, @Param("stageId") String stageId, @Param("time") Long time);

    @Modifying
    @Transactional
    @Query(value = "update FlowStage set runTime=:time, updatedTime=:time, state=:state where logId=:logId and stageId=:stageId")
    Integer finish(@Param("logId") Integer logId, @Param("stageId") String stageId, @Param("state") String state, @Param("time") Long time);

    @Modifying
    @Transactional
    @Query(value = "update FlowStage set content=:content, state=:state, updatedTime=:time where logId=:logId and stageId=:stageId")
    Integer finish(@Param("logId") Integer logId, @Param("stageId") String stageId, @Param("content") String content, @Param("state") String state, @Param("time") Long time);

    @Modifying
    @Transactional
    @Query(value = "update FlowStage set updatedTime=:time, state='TERMINATED' where logId=:logId and state in ('WAITING', 'RUNNING')")
    Integer terminal(@Param("logId") Integer logId, @Param("time") Long time);

}
