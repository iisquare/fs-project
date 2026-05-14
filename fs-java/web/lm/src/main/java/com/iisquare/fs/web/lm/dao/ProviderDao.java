package com.iisquare.fs.web.lm.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.lm.entity.Provider;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderDao extends DaoBase<Provider, Integer> {

    @Query(value = "select count(*) from Provider where serial = :serial and id != :id")
    int exist(@Param("serial") String serial, @Param("id") int id);

}
