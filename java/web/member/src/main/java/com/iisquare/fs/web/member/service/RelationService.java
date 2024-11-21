package com.iisquare.fs.web.member.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.member.dao.RelationDao;
import com.iisquare.fs.web.member.entity.Relation;
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
        return relationIds(type, aid, bids, 0);
    }

    public Set<Integer> relationIds(String type, Integer aid, Set<Integer> bids, Integer cid) {
        List<Relation> list = relationDao.findAllByTypeAndAidAndCid(type, aid, cid);
        if(null == bids) {
            return DPUtil.values(list, Integer.class, "bid");
        } else {
            relationDao.deleteAll(list);
            list = new ArrayList<>();
            for (Integer bid : bids) {
                list.add(
                        Relation.builder()
                                .id(type + "_" + aid + "_" + bid + "_" + cid)
                                .type(type).aid(aid).bid(bid).cid(cid).build());
            }
            relationDao.saveAll(list);
            return bids;
        }
    }

}
