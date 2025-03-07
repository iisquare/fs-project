package com.iisquare.fs.web.cms.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.cms.entity.Cite;

import java.util.Collection;
import java.util.List;

public interface CiteDao extends DaoBase<Cite, Integer> {

    Cite findByName(String name);

    List<Cite> findAllByNameIn(Collection<String> names);

}
