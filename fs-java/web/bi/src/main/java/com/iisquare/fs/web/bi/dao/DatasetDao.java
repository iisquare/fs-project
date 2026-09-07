package com.iisquare.fs.web.bi.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.bi.entity.Dataset;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DatasetDao extends DaoBase<Dataset, Integer> {

    @Query(value = "select count(*) from Dataset where name = :name and id != :id")
    int exist(@Param("name") String name, @Param("id") int id);

    List<Dataset> findAllByNameIn(Collection<String> names);

}
