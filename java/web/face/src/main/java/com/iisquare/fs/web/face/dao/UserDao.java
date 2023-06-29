package com.iisquare.fs.web.face.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.face.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserDao extends DaoBase<User, Integer> {

    List<User> findAllByStatusAndIdIn(Integer status, Iterable<Integer> ids);

    @Query("select id from User where status=1 and id in (:ids)")
    List<Integer> findOnline(@Param("ids") Iterable<Integer> ids);

}
