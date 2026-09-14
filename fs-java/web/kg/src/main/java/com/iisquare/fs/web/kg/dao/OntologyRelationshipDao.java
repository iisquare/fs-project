package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.OntologyRelationship;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OntologyRelationshipDao extends DaoBase<OntologyRelationship, Integer> {

    List<OntologyRelationship> findAllByOntologyIdOrderBySortAscIdAsc(Integer ontologyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from OntologyRelationship t where t.ontologyId = :ontologyId")
    int deleteByOntologyId(@Param("ontologyId") Integer ontologyId);

}
