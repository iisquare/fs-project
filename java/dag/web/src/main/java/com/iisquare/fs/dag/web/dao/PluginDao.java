package com.iisquare.fs.dag.web.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.dag.web.entity.Plugin;

import java.util.Collection;
import java.util.List;

public interface PluginDao extends DaoBase<Plugin, Integer> {

    boolean existsByNameAndStatusNotIn(String name, Collection<Integer> status);

    List<Plugin> findAllByStatusAndNameIn(Integer status, Collection<String> names);

}
