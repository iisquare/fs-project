package com.iisquare.fs.web.oa.mvc;

import com.iisquare.fs.web.oa.dsconfig.DataSourceConfig;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class WorkflowConfiguration {

    @Autowired
    private DataSourceConfig dataSourceConfig;
    @Value("${flowable.databaseSchemaUpdate:false}")
    private String databaseSchemaUpdate;

    @Bean(name = "workflowTransactionManager")
    @Qualifier("workflowTransactionManager")
    public DataSourceTransactionManager workflowTransactionManager() {
        return new DataSourceTransactionManager(dataSourceConfig.workflowDataSource());
    }

    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration() {
        SpringProcessEngineConfiguration springProcessEngineConfiguration = new SpringProcessEngineConfiguration();
        springProcessEngineConfiguration.setDataSource(dataSourceConfig.workflowDataSource());
        springProcessEngineConfiguration.setTransactionManager(workflowTransactionManager());
        // 不添加此项配置，在没创建表时，会抛出FlowableWrongDbException异常
        springProcessEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);
        return springProcessEngineConfiguration;
    }

}
