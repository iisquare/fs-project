package com.iisquare.fs.web.demo.service;

import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.demo.dao.CompositeDao;
import com.iisquare.fs.web.demo.entity.Composite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ArticleService extends ServiceBase {

    @Autowired
    private CompositeDao compositeDao;

    public Composite info(Integer aid, Integer bid) {
        Optional<Composite> info = compositeDao.findById(Composite.IdClass.builder().aid(aid).bid(bid).build());
        return info.isPresent() ? info.get() : null;
    }

}
