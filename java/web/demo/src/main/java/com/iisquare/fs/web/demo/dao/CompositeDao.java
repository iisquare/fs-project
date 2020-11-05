package com.iisquare.fs.web.demo.dao;

import com.iisquare.fs.web.demo.entity.Composite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface CompositeDao extends org.springframework.data.jpa.repository.JpaRepository<Composite, Composite.IdClass>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Composite> {

    /**
     * 自定义修改方法
     */
    @Modifying
    Number deleteAllByAid(Integer aid);

    /**
     * 自定义修改语句
     */
    @Modifying
    @Query(value = "update Composite set name=:after where name=:before")
    Integer modifyName(@Param("before") String before, @Param("after") String after);

    /**
     * 自定义删除语句
     */
    @Modifying
    @Transactional
    @Query("delete from Suggest where uid=:uid and timestamp<>:timestamp")
    Integer deleteByUidAndTimestampNot(@Param("uid") Integer uid, @Param("timestamp") Long timestamp);

    /**
     * 自定义查询方法
     */
    List<Composite> findAllByAidAndBidIn(Integer aid, Collection<Integer> bids);

    /**
     * 自定义查询语句
     */
    @Query(value = "select aid, max(bid) as mx from Composite group by aid")
    List<Object[]> findMaxBidGroupByAid();

    /**
     * 自定义分页查询
     */
    @Query(value = "select aid, bid, name from Composite where name = :name",
            countQuery = "select count(aid) from Composite where name = :name")
    Page<Object[]> findListByName(@Param("name") String name, Pageable pageable);

}
