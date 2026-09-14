package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.ExtractSource;

import java.util.List;

public interface ExtractSourceDao extends DaoBase<ExtractSource, Integer> {

    List<ExtractSource> findAllByStatusOrderByIdDesc(Integer status);

    List<ExtractSource> findAllByNameContainingAndStatusOrderByIdDesc(String name, Integer status);

}
