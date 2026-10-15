package com.iisquare.fs.web.bi.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.bi.entity.Cluster;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClusterDao extends DaoBase<Cluster, Integer> {

    @Query(value = "select count(*) from Cluster where name = :name and id != :id")
    int exist(@Param("name") String name, @Param("id") int id);

}
