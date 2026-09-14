package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.RoleResource;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface RoleResourceDao extends DaoBase<RoleResource, RoleResource.IdClass> {

    @Query("select t.resourceId from RoleResource t where t.roleId = :roleId")
    List<Integer> findResourceIdsByRoleId(@Param("roleId") Integer roleId);

    /**
     * 仅删除变化的授权，保留未改动行的审计信息
     */
    @Modifying
    @Transactional
    @Query("delete from RoleResource t where t.roleId = :roleId and t.resourceId in (:resourceIds)")
    Integer deleteByRoleIdAndResourceIdIn(
            @Param("roleId") Integer roleId, @Param("resourceIds") Collection<Integer> resourceIds);

    /**
     * 角色删除时清理授权
     */
    @Modifying
    @Transactional
    @Query("delete from RoleResource t where t.roleId in (:roleIds)")
    Integer deleteByRoleIdIn(@Param("roleIds") Collection<Integer> roleIds);

    /**
     * 资源删除时清理授权
     */
    @Modifying
    @Transactional
    @Query("delete from RoleResource t where t.resourceId in (:resourceIds)")
    Integer deleteByResourceIdIn(@Param("resourceIds") Collection<Integer> resourceIds);

}
