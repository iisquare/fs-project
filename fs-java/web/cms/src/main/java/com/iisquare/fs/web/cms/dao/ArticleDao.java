package com.iisquare.fs.web.cms.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.cms.entity.Article;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleDao extends DaoBase<Article, Integer> {

    @Query("select count(id) from Article where catalogId=:id and status<>-1")
    Number countByCatalogId(@Param("id") Integer id);

}
