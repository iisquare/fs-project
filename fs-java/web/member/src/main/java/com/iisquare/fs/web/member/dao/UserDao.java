package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.User;

import java.util.Collection;

public interface UserDao extends DaoBase<User, Integer> {

    boolean existsByNameEqualsAndIdNotIn(String name, Collection<Integer> ids);

    boolean existsBySerialEqualsAndIdNotIn(String serial, Collection<Integer> ids);

    User findFirstBySerial(String serial);

}
