package com.iisquare.fs.web.quartz.config;

import java.io.IOException;
import java.util.Properties;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class SchedulerConfig {

    @Value("${org.quartz.scheduler.instanceName}")
    private String instanceName;
    @Value("${org.quartz.scheduler.instanceId}")
    private String instanceId;
    @Value("${org.quartz.threadPool.threadCount}")
    private String threadCount;
    @Value("${org.quartz.jobStore.class}")
    private String jsClass;
    @Value("${org.quartz.jobStore.driverDelegateClass}")
    private String jsDriverDelegateClass;
    @Value("${org.quartz.jobStore.dataSource}")
    private String jsDataSource;
    @Value("${org.quartz.jobStore.misfireThreshold}")
    private String jsMisfireThreshold;
    @Value("${org.quartz.jobStore.isClustered}")
    private String jsIsClustered;
    @Value("${org.quartz.dataSource.quartz.driver}")
    private String dsDriver;
    @Value("${org.quartz.dataSource.quartz.URL}")
    private String dsUrl;
    @Value("${org.quartz.dataSource.quartz.user}")
    private String dsUser;
    @Value("${org.quartz.dataSource.quartz.password}")
    private String dsPassword;
    @Autowired
    private CustomJobFactory customJobFactory;

    @Bean(name = "Scheduler")
    public Scheduler scheduler() throws IOException {
        return schedulerFactoryBean().getScheduler();
    }

    @Bean(name = "SchedulerFactory")
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setAutoStartup(true);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setOverwriteExistingJobs(true);
        factory.setJobFactory(customJobFactory);
        factory.setQuartzProperties(quartzProperties());
        return factory;
    }

    @Bean
    public Properties quartzProperties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", instanceName);
        properties.setProperty("org.quartz.scheduler.instanceId", instanceId);
        properties.setProperty("org.quartz.threadPool.threadCount", threadCount);
        properties.setProperty("org.quartz.jobStore.class", jsClass);
        properties.setProperty("org.quartz.jobStore.driverDelegateClass", jsDriverDelegateClass);
        properties.setProperty("org.quartz.jobStore.dataSource", jsDataSource);
        properties.setProperty("org.quartz.jobStore.misfireThreshold", jsMisfireThreshold);
        properties.setProperty("org.quartz.jobStore.isClustered", jsIsClustered);
        properties.setProperty("org.quartz.dataSource.quartz.driver", dsDriver);
        properties.setProperty("org.quartz.dataSource.quartz.URL", dsUrl);
        properties.setProperty("org.quartz.dataSource.quartz.user", dsUser);
        properties.setProperty("org.quartz.dataSource.quartz.password", dsPassword);
        propertiesFactoryBean.setProperties(properties);
        propertiesFactoryBean.afterPropertiesSet();
        return propertiesFactoryBean.getObject();
    }
}
