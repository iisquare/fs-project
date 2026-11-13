package com.iisquare.fs.web.agent.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.agent.entity.AgenticLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgenticLogDao extends DaoBase<AgenticLog, Integer> {

    /**
     * 会话回看用的轻量投影：日志列表与「回复 ↔ 日志」关联只需要这些小字段，
     * 避免把 inputs / outputs / steps 三个 longtext 大字段整行读出来
     */
    interface LogIndex {
        Integer getId();
        Integer getAnswerId();
        Integer getAgenticId();
        Integer getStatus();
        Long getDuration();
        String getError();
        String getFailures();
        Long getCreatedTime();
    }

    /** 旧会话没有用户消息上下文时，从对应回复日志的 steps 恢复按模型节点展开的用户输入 */
    interface HistorySteps {
        Integer getAnswerId();
        String getSteps();
    }

    List<LogIndex> findByChatIdAndDeletedTimeOrderByIdAsc(Integer chatId, Long deletedTime);

    @Query("select l.answerId as answerId, l.steps as steps from AgenticLog l "
            + "where l.chatId = :chatId and l.deletedTime = 0 and l.answerId > 0 order by l.id asc")
    List<HistorySteps> findHistoryStepsByChatId(@Param("chatId") Integer chatId);
}
