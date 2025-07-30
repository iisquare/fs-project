package com.iisquare.fs.base.jpa.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceBuilder;
import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class DruidConfiguration {

    @Value("${fs.druid.registration:/druid/*}")
    private String registration;
    @Value("${fs.druid.allow:127.0.0.1}")
    private String allow;
    @Value("${fs.druid.deny:}")
    private String deny;
    @Value("${fs.druid.loginUsername:admin}")
    private String loginUsername;
    @Value("${fs.druid.loginPassword:admin888}")
    private String loginPassword;
    @Value("${fs.druid.resetEnable:false}")
    private String resetEnable;
    @Value("${fs.druid.sessionStat:true}")
    private String sessionStat;

    /**
     * 注册 StatViewServlet（监控页面）
     */
    @Bean
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
        ServletRegistrationBean<StatViewServlet> bean =
                new ServletRegistrationBean<>(new StatViewServlet(), registration);
        // 添加初始化参数：initParams
        if (!DPUtil.empty(allow)) bean.addInitParameter("allow", allow); // 白名单
        if (!DPUtil.empty(deny)) bean.addInitParameter("deny", deny); // IP黑名单 (存在共同时，deny优先于allow)
        // 登录查看信息的账号密码.
        bean.addInitParameter("loginUsername", loginUsername);
        bean.addInitParameter("loginPassword", loginPassword);
        // 是否能够重置数据.
        bean.addInitParameter("resetEnable", resetEnable);
        return bean;
    }

    /**
     * 注册 WebStatFilter（监控采集）
     */
    @Bean
    public FilterRegistrationBean<WebStatFilter> druidWebStatFilter() {
        FilterRegistrationBean<WebStatFilter> bean =
                new FilterRegistrationBean<>(new WebStatFilter());
        // 添加过滤规则
        bean.addUrlPatterns("/*");
        // 忽略过滤格式
        bean.addInitParameter("exclusions","*.js,*.gif,*.jpg,*.png,*.css,*.ico," + registration);
        // 是否监控session
        bean.addInitParameter("sessionStatEnable", sessionStat);
        return bean;
    }

    public static DataSource createDataSource() throws SQLException {
        DruidDataSource source = DruidDataSourceBuilder.create().build();
        source.setUseGlobalDataSourceStat(true);
        source.setFilters("stat");
        return source;
    }

}
