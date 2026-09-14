package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.RoleApplication;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface RoleApplicationDao extends DaoBase<RoleApplication, RoleApplication.IdClass> {

    @Query("select t.applicationId from RoleApplication t where t.roleId = :roleId")
    List<Integer> findApplicationIdsByRoleId(@Param("roleId") Integer roleId);

    List<RoleApplication> findAllByRoleIdIn(Collection<Integer> roleIds);

    /**
     * 仅删除变化的授权，保留未改动行的审计信息
     */
    @Modifying
    @Transactional
    @Query("delete from RoleApplication t where t.roleId = :roleId and t.applicationId in (:applicationIds)")
    Integer deleteByRoleIdAndApplicationIdIn(
            @Param("roleId") Integer roleId, @Param("applicationIds") Collection<Integer> applicationIds);

    /**
     * 角色删除时清理授权
     */
    @Modifying
    @Transactional
    @Query("delete from RoleApplication t where t.roleId in (:roleIds)")
    Integer deleteByRoleIdIn(@Param("roleIds") Collection<Integer> roleIds);

    /**
     * 应用删除时清理授权
     */
    @Modifying
    @Transactional
    @Query("delete from RoleApplication t where t.applicationId in (:applicationIds)")
    Integer deleteByApplicationIdIn(@Param("applicationIds") Collection<Integer> applicationIds);

}
