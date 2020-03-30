package com.iisquare.fs.web.quartz.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.quartz.entity.RunLog;

import java.util.List;

public interface RunLogDao extends DaoBase<RunLog, Integer> {

    RunLog findByTag(String tag);

    List<RunLog> findByResult(Integer result);
}
