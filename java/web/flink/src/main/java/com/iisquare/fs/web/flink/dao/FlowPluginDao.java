package com.iisquare.fs.web.flink.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.flink.entity.FlowPlugin;

import java.util.Collection;
import java.util.List;

public interface FlowPluginDao extends DaoBase<FlowPlugin, Integer> {

    List<FlowPlugin> findAllByStatusAndNameIn(Integer status, Collection<String> names);

}
