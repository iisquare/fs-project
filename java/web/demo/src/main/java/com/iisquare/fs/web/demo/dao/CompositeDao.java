package com.iisquare.fs.web.demo.dao;

import com.iisquare.fs.web.demo.entity.Composite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * @see(https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
 */
public interface CompositeDao extends JpaRepository<Composite, Composite.IdClass>, JpaSpecificationExecutor<Composite> {

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

    @Query("select t from Composite t where t.name = :name")
    Composite findByName(@Param("name") String name);

    @Query("from Composite where name <> :name")
    Composite findByNameNot(@Param("name") String name);

    @Query("from Composite where name in(:name)")
    List<Composite> findAllByNameIn(@Param("name") Collection<String> name);

    /**
     * Upsert更新插入
     */
    @Modifying
    @Query(value = "INSERT INTO t_composite (aid, bid, created_time, updated_time)" +
            " VALUES (:#{#i.aid}, :#{#i.bid}, :#{#i.createdTime}, :#{#i.updatedTime})" +
            " ON DUPLICATE KEY UPDATE updated_time=VALUES(updated_time)", nativeQuery = true)
    Integer record(@Param("i") Composite i);

}
