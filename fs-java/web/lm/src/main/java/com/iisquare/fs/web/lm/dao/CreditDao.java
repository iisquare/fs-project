package com.iisquare.fs.web.lm.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.lm.entity.Credit;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface CreditDao extends DaoBase<Credit, Integer> {

    @Modifying
    @Query(value = "update Credit set remained=remained - :amount, consumed=consumed + :amount where uid=:uid")
    int consume(@Param("uid") Integer uid, @Param("amount") BigDecimal amount);

    @Modifying
    @Query(value = "update Credit set remained=remained - :amount, consumed=consumed + :amount where uid=:uid and remained >= :amount")
    int consumeWithCheck(@Param("uid") Integer uid, @Param("amount") BigDecimal amount);

}
