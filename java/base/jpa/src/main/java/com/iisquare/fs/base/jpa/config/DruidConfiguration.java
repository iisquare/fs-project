package com.iisquare.fs.base.jpa.config;

import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    @Value("${fs.druid.sessionStat:false}")
    private String sessionStat;

    /**
     * 注册一个StatViewServlet
     */
    @Bean
    public ServletRegistrationBean DruidStatViewServlet2(){
        // org.springframework.boot.context.embedded.ServletRegistrationBean提供类的进行注册.
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new StatViewServlet(), registration);
        // 添加初始化参数：initParams
        if (!DPUtil.empty(allow)) servletRegistrationBean.addInitParameter("allow", allow); // 白名单
        if (!DPUtil.empty(deny)) servletRegistrationBean.addInitParameter("deny", deny); // IP黑名单 (存在共同时，deny优先于allow)
        // 登录查看信息的账号密码.
        servletRegistrationBean.addInitParameter("loginUsername", loginUsername);
        servletRegistrationBean.addInitParameter("loginPassword", loginPassword);
        // 是否能够重置数据.
        servletRegistrationBean.addInitParameter("resetEnable", resetEnable);
        return servletRegistrationBean;
    }

    /**
     * 注册一个：filterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean druidStatFilter2(){

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new WebStatFilter());

        // 添加过滤规则
        filterRegistrationBean.addUrlPatterns("/*");

        // 忽略过滤格式
        filterRegistrationBean.addInitParameter("exclusions","*.js,*.gif,*.jpg,*.png,*.css,*.ico," + registration);

        // 是否监控session
        filterRegistrationBean.addInitParameter("sessionStatEnable", sessionStat);
        return filterRegistrationBean;
    }

}
