package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.OntologyRelationshipField;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;

public interface OntologyRelationshipFieldDao extends DaoBase<OntologyRelationshipField, Integer> {

    List<OntologyRelationshipField> findAllByRelationshipIdOrderBySortAscIdAsc(Integer relationshipId);

    List<OntologyRelationshipField> findAllByRelationshipIdInOrderBySortAscIdAsc(Collection<Integer> relationshipIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from OntologyRelationshipField t where t.relationshipId = :relationshipId")
    int deleteByRelationshipId(@Param("relationshipId") Integer relationshipId);

}
