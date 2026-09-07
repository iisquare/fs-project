package com.iisquare.fs.web.bi.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.bi.entity.DataApi;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataApiDao extends DaoBase<DataApi, Integer> {

    @Query(value = "select count(*) from DataApi where name = :name and id != :id")
    int exist(@Param("name") String name, @Param("id") int id);

    DataApi findByName(String name);
}
