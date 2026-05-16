package com.iisquare.fs.web.cms.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.cms.entity.Catalog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogDao extends DaoBase<Catalog, Integer> {

    @Query("select count(id) from Catalog where parentId=:id and status<>-1")
    Number childrenCount(@Param("id") Integer id);

}
