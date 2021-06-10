package com.iisquare.fs.web.oa.dsconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "oaEntityManagerFactory",
    transactionManagerRef = "oaTransactionManager",
    basePackages = {"com.iisquare.fs.web.oa.dao"}
)
public class OADSConfig extends DSConfigBase {

    @Autowired
    private NamingStrategy namingStrategy;

    @Bean(name = "oaEntityManager")
    public EntityManager oaEntityManager(EntityManagerFactoryBuilder builder) {
        return oaEntityManagerFactory(builder).getObject().createEntityManager();
    }

    protected Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.physical_naming_strategy", namingStrategy);
        return props;
    }

    @Bean(name = "oaEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean oaEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
            .dataSource(dataSourceConfig.oaDataSource())
            .packages("com.iisquare.fs.web.oa.entity") //设置实体类所在位置
            .persistenceUnit("oaPersistenceUnit").properties(jpaProperties()).build();
    }

    @Primary
    @Bean(name = "oaTransactionManager")
    public PlatformTransactionManager oaTransactionManager(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(oaEntityManagerFactory(builder).getObject());
    }

}
