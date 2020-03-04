package com.iisquare.fs.web.admin.dao;

import com.iisquare.fs.web.admin.entity.User;
import com.iisquare.fs.base.jpa.mvc.DaoBase;

import java.util.Collection;

public interface UserDao extends DaoBase<User, Integer> {

    boolean existsByNameEqualsAndIdNotIn(String name, Collection<Integer> ids);

    boolean existsBySerial(String serial);

    User findFirstBySerial(String serial);

}
