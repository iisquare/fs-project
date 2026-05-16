package com.iisquare.fs.web.lucene.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.lucene.entity.Dictionary;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DictionaryDao extends DaoBase<Dictionary, Integer> {

    @Transactional
    @Modifying
    @Query(value = "delete from fs_lucene_dictionary where catalogue=:catalogue and type=:type and id not in (" +
            "select * from (select max(id) from fs_lucene_dictionary" +
            " where catalogue=:catalogue and type=:type GROUP BY content HAVING count(content) > 1" +
            ") as a) and content in (select * from (select content from fs_lucene_dictionary" +
            " where catalogue=:catalogue and type=:type GROUP BY content HAVING count(content) > 1" +
            ") as b)", nativeQuery = true)
    Integer unique(@Param("catalogue") String catalogue, @Param("type") String type);

}
