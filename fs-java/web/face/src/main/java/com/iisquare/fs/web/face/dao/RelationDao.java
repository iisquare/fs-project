package com.iisquare.fs.web.face.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.face.entity.Relation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RelationDao extends DaoBase<Relation, String> {

    List<Relation> findAllByTypeAndAid(String type, Integer aid);

    List<Relation> findAllByTypeAndAidIn(String type, Collection<Integer> aids);

    List<Relation> findAllByTypeAndBidIn(String type, Collection<Integer> bids);

    @Query("select bid from Relation  where type=:type and aid in (:aids)")
    List<Integer> findBidByAid(@Param("type") String type, @Param("aids") Collection<Integer> aids);

    @Query("select aid from Relation  where type=:type and bid in (:bids)")
    List<Integer> findAidByBid(@Param("type") String type, @Param("bids") Collection<Integer> bids);

}
