package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.RoleMenu;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface RoleMenuDao extends DaoBase<RoleMenu, RoleMenu.IdClass> {

    @Query("select t.menuId from RoleMenu t where t.roleId = :roleId")
    List<Integer> findMenuIdsByRoleId(@Param("roleId") Integer roleId);

    List<RoleMenu> findAllByRoleIdIn(Collection<Integer> roleIds);

    /**
     * 仅删除变化的授权，保留未改动行的审计信息
     */
    @Modifying
    @Transactional
    @Query("delete from RoleMenu t where t.roleId = :roleId and t.menuId in (:menuIds)")
    Integer deleteByRoleIdAndMenuIdIn(@Param("roleId") Integer roleId, @Param("menuIds") Collection<Integer> menuIds);

    /**
     * 角色删除时清理授权
     */
    @Modifying
    @Transactional
    @Query("delete from RoleMenu t where t.roleId in (:roleIds)")
    Integer deleteByRoleIdIn(@Param("roleIds") Collection<Integer> roleIds);

    /**
     * 菜单删除时清理授权
     */
    @Modifying
    @Transactional
    @Query("delete from RoleMenu t where t.menuId in (:menuIds)")
    Integer deleteByMenuIdIn(@Param("menuIds") Collection<Integer> menuIds);

}
