package com.iisquare.fs.cloud.provider;

import com.iisquare.fs.base.web.mvc.BeanNameGenerator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
    "com.iisquare.fs.base.core.*",
    "com.iisquare.fs.base.jpa.*",
    "com.iisquare.fs.base.swagger.*",
    "com.iisquare.fs.cloud.provider"
})
@EnableHystrix
public class ProviderApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ProviderApplication.class).beanNameGenerator(new BeanNameGenerator()).run(args);
    }

}
