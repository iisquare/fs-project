package com.iisquare.fs.web.bi.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.bi.entity.Job;

import java.util.Collection;
import java.util.List;

public interface JobDao extends DaoBase<Job, Integer> {

    List<Job> findAllByStateIn(Collection<String> states);

    int countByDiagramIdAndState(Integer diagramId, String state);

}
