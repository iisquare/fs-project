package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.FusionRule;

import java.util.List;

public interface FusionRuleDao extends DaoBase<FusionRule, Integer> {

    List<FusionRule> findAllByStatusOrderByIdDesc(Integer status);

}
