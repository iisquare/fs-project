package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.DataQuery;

import java.util.List;
import java.util.Optional;

public interface DataQueryDao extends DaoBase<DataQuery, Integer> {

    List<DataQuery> findAllByOntologyIdAndKindAndLabelAndUidOrderByIdDesc(
            Integer ontologyId, String kind, String label, Integer uid);

    Optional<DataQuery> findFirstByOntologyIdAndKindAndLabelAndUidAndName(
            Integer ontologyId, String kind, String label, Integer uid, String name);

}
