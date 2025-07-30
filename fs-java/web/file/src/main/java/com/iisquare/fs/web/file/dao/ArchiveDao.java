package com.iisquare.fs.web.file.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.file.entity.Archive;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;

public interface ArchiveDao extends DaoBase<Archive, String> {

    @Modifying
    @Transactional
    @Query(value = "update Archive set status=4 where id=:id and status=1")
    Integer miss(@Param("id") String id);

}
