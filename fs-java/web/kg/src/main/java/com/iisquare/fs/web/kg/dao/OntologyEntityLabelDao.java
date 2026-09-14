package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.OntologyEntityLabel;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;

public interface OntologyEntityLabelDao extends DaoBase<OntologyEntityLabel, Integer> {

    List<OntologyEntityLabel> findAllByEntityIdOrderBySortAscIdAsc(Integer entityId);

    List<OntologyEntityLabel> findAllByEntityIdInOrderBySortAscIdAsc(Collection<Integer> entityIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from OntologyEntityLabel t where t.entityId = :entityId")
    int deleteByEntityId(@Param("entityId") Integer entityId);

}
