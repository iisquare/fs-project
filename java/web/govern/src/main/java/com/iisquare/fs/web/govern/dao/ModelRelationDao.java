package com.iisquare.fs.web.govern.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.govern.entity.ModelRelation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface ModelRelationDao extends DaoBase<ModelRelation, Integer> {

    @Modifying
    @Transactional
    @Query("delete from ModelRelation where " +
            "(sourceCatalog=:catalog and sourceModel=:model) or (targetCatalog=:catalog and targetModel=:model)")
    Integer deleteByCatalogAndModel(@Param("catalog") String catalog, @Param("model") String model);

    @Modifying
    @Transactional
    @Query("delete from ModelRelation where sourceCatalog like :catalog or targetCatalog like :catalog")
    Integer deleteByCatalog(@Param("catalog") String catalog);

    @Query(value = "select relation as relation, count(relation) as ct from ModelRelation group by relation order by ct desc")
    List<Map<String, Object>> relations();

    @Query(value = "select t.sourceCatalog as sourceCatalog, t.sourceModel as sourceModel, count(t) as ct" +
            " from ModelRelation t where t.sourceColumn = '' group by t.sourceCatalog, t.sourceModel")
    List<Map<String, Object>> modelSources(Pageable pageable);

    @Query(value = "select t.sourceCatalog as sourceCatalog, t.sourceModel as sourceModel, t.sourceColumn as sourceColumn, count(t) as ct" +
            " from ModelRelation t where t.sourceColumn <> '' group by t.sourceCatalog, t.sourceModel, t.sourceColumn")
    List<Map<String, Object>> columnSources(Pageable pageable);

    @Query(value = "select t.targetCatalog as targetCatalog, t.targetModel as targetModel, count(t) as ct" +
            " from ModelRelation t where t.targetColumn = '' group by t.targetCatalog, t.targetModel")
    List<Map<String, Object>> modelTargets(Pageable pageable);

    @Query(value = "select t.targetCatalog as targetCatalog, t.targetModel as targetModel, t.targetColumn as targetColumn, count(t) as ct" +
            " from ModelRelation t where t.targetColumn <> '' group by t.targetCatalog, t.targetModel, t.targetColumn")
    List<Map<String, Object>> columnTargets(Pageable pageable);

}
