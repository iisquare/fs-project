package com.iisquare.fs.web.lm.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.lm.entity.Server;

import java.util.Collection;

public interface ServerDao extends DaoBase<Server, Integer> {

    boolean existsByModelEqualsAndIdNotIn(String model, Collection<Integer> ids);

}
