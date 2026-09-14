package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface RoleDao extends DaoBase<Role, Integer> {

    /**
     * 全部角色标识，用于批量清理资源缓存
     */
    @Query("select t.id from Role t")
    List<Integer> findAllIds();

    /**
     * 悲观锁，串行化同一角色的授权变更
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Role t where t.id = :id")
    Optional<Role> findByIdForUpdate(@Param("id") Integer id);

}
