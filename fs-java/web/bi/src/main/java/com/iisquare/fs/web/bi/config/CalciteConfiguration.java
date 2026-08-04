package com.iisquare.fs.web.bi.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.calcite.core.CalciteSession;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.dao.DatasourceDao;
import com.iisquare.fs.web.bi.entity.Datasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CalciteConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CalciteConfiguration.class);

    @Autowired
    DatasourceDao datasourceDao;

    @Bean
    public CalciteSession calciteSession() throws Exception {
        CalciteSession session = new CalciteSession();
        // Load all active JDBC datasources
        List<Datasource> list = datasourceDao.findAll();
        int registered = 0;
        for (Datasource ds : list) {
            if (1 != ds.getStatus()) continue;
            if (!isJdbcType(ds.getType())) continue;
            try {
                JsonNode config = DPUtil.parseJSON(ds.getContent());
                if (null == config) {
                    logger.warn("Failed to parse datasource config for id={}, name={}", ds.getId(), ds.getName());
                    continue;
                }
                String alias = "ds_" + ds.getId();
                session.jdbc(alias, config);
                registered++;
                logger.info("Registered Calcite schema '{}' for datasource id={}, name={}, type={}",
                        alias, ds.getId(), ds.getName(), ds.getType());
            } catch (Exception e) {
                logger.error("Failed to register datasource id={}, name={}, type={}: {}",
                        ds.getId(), ds.getName(), ds.getType(), e.getMessage());
            }
        }
        logger.info("Calcite session initialized with {}/{} JDBC datasources registered",
                registered, list.stream().filter(d -> 1 == d.getStatus() && isJdbcType(d.getType())).count());
        return session;
    }

    private boolean isJdbcType(String type) {
        return "MySQL".equals(type) || "PostgreSQL".equals(type) || "Oracle".equals(type)
                || "SQLServer".equals(type) || "ClickHouse".equals(type);
    }

}
