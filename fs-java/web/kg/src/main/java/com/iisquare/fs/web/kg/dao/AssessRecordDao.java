package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.AssessRecord;

import java.util.List;

public interface AssessRecordDao extends DaoBase<AssessRecord, Integer> {

    List<AssessRecord> findAllByOntologyIdOrderByIdDesc(Integer ontologyId);

}
