package com.iisquare.fs.web.cms.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.cms.entity.Tag;

import java.util.Collection;
import java.util.List;

public interface TagDao extends DaoBase<Tag, Integer> {

    Tag findByName(String name);

    List<Tag> findAllByNameIn(Collection<String> names);

}
