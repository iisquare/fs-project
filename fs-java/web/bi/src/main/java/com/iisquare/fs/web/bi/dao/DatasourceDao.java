package com.iisquare.fs.web.bi.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.bi.entity.Datasource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DatasourceDao extends DaoBase<Datasource, Integer> {

    @Query(value = "select count(*) from Datasource where name = :name and id != :id")
    int exist(@Param("name") String name, @Param("id") int id);

}
