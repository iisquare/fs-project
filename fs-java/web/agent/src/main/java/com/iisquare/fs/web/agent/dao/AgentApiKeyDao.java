package com.iisquare.fs.web.agent.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.agent.entity.AgentApiKey;

import java.util.Optional;

public interface AgentApiKeyDao extends DaoBase<AgentApiKey, Integer> {

    Optional<AgentApiKey> findFirstBySecretAndDeletedTime(String secret, Long deletedTime);

}
