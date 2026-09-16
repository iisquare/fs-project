package com.iisquare.fs.web.agent.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.agent.entity.KnowledgeImage;

import java.util.List;

public interface KnowledgeImageDao extends DaoBase<KnowledgeImage, String> {

    List<KnowledgeImage> findAllByDocumentIdIn(List<Integer> documentIds);

    List<KnowledgeImage> findAllByKnowledgeIdIn(List<Integer> knowledgeIds);

}
