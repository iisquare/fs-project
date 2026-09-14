package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.UserRole;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface UserRoleDao extends DaoBase<UserRole, UserRole.IdClass> {

    /**
     * 全部角色标识，不按角色状态过滤，角色有效性由角色缓存中的状态决定
     */
    @Query("select t.roleId from UserRole t where t.userId = :userId")
    List<Integer> findRoleIdsByUserId(@Param("userId") Integer userId);

    List<UserRole> findAllByUserIdIn(Collection<Integer> userIds);

    List<UserRole> findAllByRoleIdIn(Collection<Integer> roleIds);

    /**
     * 仅删除变化的授权，保留未改动行的审计信息
     */
    @Modifying
    @Transactional
    @Query("delete from UserRole t where t.userId = :userId and t.roleId in (:roleIds)")
    Integer deleteByUserIdAndRoleIdIn(@Param("userId") Integer userId, @Param("roleIds") Collection<Integer> roleIds);

    /**
     * 角色删除时清理授权
     */
    @Modifying
    @Transactional
    @Query("delete from UserRole t where t.roleId in (:roleIds)")
    Integer deleteByRoleIdIn(@Param("roleIds") Collection<Integer> roleIds);

}
