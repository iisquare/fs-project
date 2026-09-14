package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
public interface UserDao extends DaoBase<User, Integer> {

    /**
     * 悲观锁，串行化同一用户的授权变更
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from User t where t.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Integer id);

}
