package com.iisquare.fs.cloud.provider.service.second;

import com.iisquare.fs.cloud.provider.dao.second.SecondRoleDao;
import com.iisquare.fs.cloud.provider.entity.second.SecondRole;
import com.iisquare.fs.base.jpa.helper.SQLHelper;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

@Service
public class RoleService extends ServiceBase {

    @Autowired
    private SecondRoleDao secondHouseDao;
    @Autowired
    @Qualifier("secondEntityManager")
    private EntityManager secondEntityManager;

    public SecondRole info(String prefix, Integer id) {
        SQLHelper helper = SQLHelper.build(secondEntityManager, prefix + "_role");
        helper.where("id=:id", "id", id);
        return helper.one(SecondRole.class);
    }

}
