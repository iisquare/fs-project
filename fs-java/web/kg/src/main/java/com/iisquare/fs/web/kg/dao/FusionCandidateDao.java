package com.iisquare.fs.web.kg.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.kg.entity.FusionCandidate;

import java.util.List;

public interface FusionCandidateDao extends DaoBase<FusionCandidate, Integer> {

    List<FusionCandidate> findAllByOntologyIdAndEntityLabel(Integer ontologyId, String entityLabel);

}
