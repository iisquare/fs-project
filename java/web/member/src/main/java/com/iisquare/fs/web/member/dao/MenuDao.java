package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.Menu;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface MenuDao extends DaoBase<Menu, Integer> {
    @Modifying
    @Transactional
    @Query("update Menu set status = -1 where applicationId in (:ids)")
    Integer deleteByApplicationIds(@Param("ids") Collection<Integer> ids);
}
