package com.iisquare.fs.web.govern.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.govern.entity.Standard;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StandardDao extends DaoBase<Standard, Standard.IdClass> {

    @Modifying
    @Transactional
    @Query("delete from Standard where catalog like :catalog")
    Integer deleteByCatalog(@Param("catalog") String catalog);

}
