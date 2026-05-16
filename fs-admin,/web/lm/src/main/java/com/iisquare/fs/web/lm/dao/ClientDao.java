package com.iisquare.fs.web.lm.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.lm.entity.Client;

import java.util.Collection;

public interface ClientDao extends DaoBase<Client, Integer> {

    boolean existsByTokenEqualsAndIdNotIn(String token, Collection<Integer> ids);

}
