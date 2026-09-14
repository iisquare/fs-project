package com.iisquare.fs.web.agent;

import com.iisquare.fs.base.web.mvc.BeanNameGenerator;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 智能体服务：智能体应用、模型对话、知识库、插件管理（工具、技能、MCP）
 *
 * 数据源取 spring.datasource.agent（表前缀 fs_agent_），模型调用经模型网关 rpc.lm.rest
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.iisquare.fs.base.core.*",
        "com.iisquare.fs.base.jpa.*",
        "com.iisquare.fs.base.elasticsearch.*",
        "com.iisquare.fs.web.core.*",
        "com.iisquare.fs.web.agent",
})
@EntityScan(basePackages = {"com.iisquare.fs.web.agent.entity"})
@EnableJpaRepositories(basePackages = {"com.iisquare.fs.web.agent.dao"})
@EnableFeignClients(basePackages = {
        "com.iisquare.fs.web.core.rpc"
}, defaultConfiguration = { FeignInterceptor.class })
public class AgentApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(AgentApplication.class).beanNameGenerator(new BeanNameGenerator()).run(args);
    }

}
