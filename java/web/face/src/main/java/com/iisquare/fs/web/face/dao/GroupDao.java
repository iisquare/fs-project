package com.iisquare.fs.web.face.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.face.entity.Group;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupDao extends DaoBase<Group, Integer> {

    @Query("select id from Group where status=1")
    List<Integer> findOnline();

}
