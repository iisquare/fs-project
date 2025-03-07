package com.iisquare.fs.web.oa.dsconfig;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class DSConfigBase {

    @Autowired
    protected DataSourceConfig dataSourceConfig;

}
