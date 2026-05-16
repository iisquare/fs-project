package com.iisquare.fs.web.face.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.face.entity.Photo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PhotoDao extends DaoBase<Photo, Integer> {

    @Transactional
    @Modifying
    @Query(value = "update Photo set cover=0 where userId=:userId")
    Integer revertCoverByUserId(@Param("userId") Integer userId);

    @Query(value = "select new Photo(id, userId, face) from Photo where cover=1 and status=1 and userId in (:userIds)")
    List<Photo> findCoverByUserId(@Param("userIds") Iterable<Integer> userIds);

    @Query(value = "select new Photo(userId, eigenvalue) from Photo where status=1 and userId in (:userIds)")
    List<Photo> findOnline(@Param("userIds") Iterable<Integer> userIds);

}
