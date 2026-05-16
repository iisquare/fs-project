package com.iisquare.fs.web.govern.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.govern.entity.ModelColumn;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface ModelColumnDao extends DaoBase<ModelColumn, ModelColumn.IdClass> {

    List<ModelColumn> findAllByCatalogAndModel(String catalog, String model, Sort sort);

    @Modifying
    @Transactional
    @Query("delete from ModelColumn where catalog=:catalog and model=:model")
    Integer deleteByCatalogAndModel(@Param("catalog") String catalog, @Param("model") String model);

    @Modifying
    @Transactional
    @Query("delete from ModelColumn where catalog like :catalog")
    Integer deleteByCatalog(@Param("catalog") String catalog);

    @Query(value = "select type as type, count(type) as ct from ModelColumn group by type")
    List<Map<String, Object>> types(Pageable pageable);

}
