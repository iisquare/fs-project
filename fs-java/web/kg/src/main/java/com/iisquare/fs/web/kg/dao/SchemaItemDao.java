package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.SchemaItem;

import java.util.List;
import java.util.Optional;

public interface SchemaItemDao extends DaoBase<SchemaItem, Integer> {

    Optional<SchemaItem> findByName(String name);

    List<SchemaItem> findAllByStatus(Integer status);

    List<SchemaItem> findAllByOntologyIdAndStatus(Integer ontologyId, Integer status);

}
