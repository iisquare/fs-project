package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.OntologyEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OntologyEntityDao extends DaoBase<OntologyEntity, Integer> {

    List<OntologyEntity> findAllByOntologyIdOrderBySortAscIdAsc(Integer ontologyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from OntologyEntity t where t.ontologyId = :ontologyId")
    int deleteByOntologyId(@Param("ontologyId") Integer ontologyId);

}
