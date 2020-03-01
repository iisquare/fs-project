package com.iisquare.fs.web.admin.dao;

import com.iisquare.fs.web.admin.entity.Relation;
import com.iisquare.fs.base.jpa.mvc.DaoBase;

import java.util.Collection;
import java.util.List;

public interface RelationDao extends DaoBase<Relation, String> {

    List<Relation> findAllByTypeAndAid(String type, Integer aid);

    List<Relation> findAllByTypeAndAidIn(String type, Collection<Integer> aids);

    List<Relation> findAllByTypeAndBidIn(String type, Collection<Integer> bids);

}
