package com.iisquare.fs.web.flink.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.flink.entity.Plugin;

import java.util.Collection;

public interface PluginDao extends DaoBase<Plugin, Integer> {

    boolean existsByNameAndStatusNotIn(String name, Collection<Integer> status);

}
