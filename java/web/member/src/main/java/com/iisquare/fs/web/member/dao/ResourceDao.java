package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.Resource;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface ResourceDao extends DaoBase<Resource, Integer> {
    @Modifying
    @Transactional
    @Query("update Resource set status = -1 where applicationId in (:ids)")
    Integer deleteByApplicationIds(@Param("ids") Collection<Integer> ids);
}
