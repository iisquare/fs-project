package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.Setting;

public interface SettingDao extends DaoBase<Setting, Integer> {

    Setting findFirstByTypeAndName(String type, String name);

}
