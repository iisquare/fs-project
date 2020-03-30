package com.iisquare.fs.web.admin.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.admin.entity.FlowPlugin;

import java.util.Collection;

public interface FlowPluginDao extends DaoBase<FlowPlugin, Integer> {

    boolean existsByNameAndStatusNotIn(String name, Collection<Integer> status);

}
