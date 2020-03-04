package com.iisquare.fs.web.admin.service;

import com.iisquare.fs.web.admin.dao.RelationDao;
import com.iisquare.fs.web.admin.entity.Relation;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RelationService extends ServiceBase {

    @Autowired
    private RelationDao relationDao;

    public Set<Integer> relationIds(String type, Integer aid, Set<Integer> bids) {
        List<Relation> list = relationDao.findAllByTypeAndAid(type, aid);
        if(null == bids) {
            return ServiceUtil.getPropertyValues(list, Integer.class, "bid");
        } else {
            relationDao.deleteAll(list);
            list = new ArrayList<>();
            for (Integer bid : bids) {
                list.add(Relation.builder().id(type + "_" + aid + "_" + bid).type(type).aid(aid).bid(bid).build());
            }
            relationDao.saveAll(list);
            return bids;
        }
    }

}
