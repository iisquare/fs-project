package com.iisquare.fs.cloud.provider.service.primary;

import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.cloud.provider.dao.primary.PrimaryUserDao;
import com.iisquare.fs.cloud.provider.entity.primary.PrimaryUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceBase {

    @Autowired
    private PrimaryUserDao primaryUserDao;

    public PrimaryUser info(Integer id) {
        return JPAUtil.findById(primaryUserDao, id, PrimaryUser.class);

    }

}
