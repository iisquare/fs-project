package com.iisquare.fs.web.agent.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.agent.entity.CompareCall;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 调用明细的数据访问：统计页的两张聚合表都从这里出。
 * 用聚合投影而不是把明细全读回来在 Java 里算——量一大就不行了。
 *
 * 筛选条件统一用「空串 / 0 即不过滤」的写法，省得为每种组合各写一条查询。
 */
public interface CompareCallDao extends DaoBase<CompareCall, Integer> {

    /**
     * 某轮提交的调用明细，按「发起时间 + id」升序：与页面上卡片的顺序一致。
     *
     * 不能按 id 排：明细是各条流并行写进来的，谁先结束谁先落库，
     * 插入顺序并不等于提交顺序（恢复历史时卡片会跟着错位）。
     */
    List<CompareCall> findByRunIdOrderByCreatedTimeAscIdAsc(Integer runId);

    /** 按模型统计 */
    interface ModelStat {
        String getModel();
        Long getTotal();
        Long getFinished();
        Long getAborted();
        Long getErrored();
        Double getAvgDuration();
        Double getAvgFirstToken();
    }

    @Query("select c.model as model, count(c) as total, "
            + "sum(case when c.state = 'finish' then 1 else 0 end) as finished, "
            + "sum(case when c.state = 'abort' then 1 else 0 end) as aborted, "
            + "sum(case when c.state = 'error' then 1 else 0 end) as errored, "
            + "avg(c.duration) as avgDuration, avg(c.firstToken) as avgFirstToken "
            + "from CompareCall c "
            + "where c.createdTime >= :beginTime and c.createdTime <= :endTime "
            + "and (:model = '' or c.model = :model) and (:uid = 0 or c.createdUid = :uid) "
            + "group by c.model order by count(c) desc")
    List<ModelStat> statByModel(@Param("beginTime") Long beginTime, @Param("endTime") Long endTime,
                                @Param("model") String model, @Param("uid") Integer uid);

    /** 按用户统计 */
    interface UserStat {
        Integer getCreatedUid();
        Long getTotal();
        Long getModelCount();
    }

    @Query("select c.createdUid as createdUid, count(c) as total, count(distinct c.model) as modelCount "
            + "from CompareCall c "
            + "where c.createdTime >= :beginTime and c.createdTime <= :endTime "
            + "and (:model = '' or c.model = :model) and (:uid = 0 or c.createdUid = :uid) "
            + "group by c.createdUid order by count(c) desc")
    List<UserStat> statByUser(@Param("beginTime") Long beginTime, @Param("endTime") Long endTime,
                              @Param("model") String model, @Param("uid") Integer uid);

    /** 「用户 × 模型」的交汇数量：用户维度的堆叠柱状图靠它出数据 */
    interface UserModelStat {
        Integer getCreatedUid();
        String getModel();
        Long getTotal();
    }

    @Query("select c.createdUid as createdUid, c.model as model, count(c) as total "
            + "from CompareCall c "
            + "where c.createdTime >= :beginTime and c.createdTime <= :endTime "
            + "and (:model = '' or c.model = :model) and (:uid = 0 or c.createdUid = :uid) "
            + "group by c.createdUid, c.model order by count(c) desc")
    List<UserModelStat> statByUserModel(@Param("beginTime") Long beginTime, @Param("endTime") Long endTime,
                                        @Param("model") String model, @Param("uid") Integer uid);

}
