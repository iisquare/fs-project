package com.iisquare.fs.web.admin.dao;

import com.iisquare.fs.web.admin.entity.Setting;
import com.iisquare.fs.base.jpa.mvc.DaoBase;

public interface SettingDao extends DaoBase<Setting, Integer> {

    Setting findFirstByTypeAndName(String type, String name);

}
