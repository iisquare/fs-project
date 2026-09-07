package com.iisquare.fs.web.bi.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.bi.entity.DataTheme;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataThemeDao extends DaoBase<DataTheme, Integer> {

    @Query(value = "select count(*) from DataTheme where name = :name and id != :id")
    int exist(@Param("name") String name, @Param("id") int id);

}
