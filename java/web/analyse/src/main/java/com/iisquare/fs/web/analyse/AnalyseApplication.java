package com.iisquare.fs.web.analyse;

import com.iisquare.fs.base.web.mvc.BeanNameGenerator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
    "com.iisquare.fs.base.core.*",
    "com.iisquare.fs.base.jpa.*",
    "com.iisquare.fs.base.swagger.*"
})
public class AnalyseApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(AnalyseApplication.class).beanNameGenerator(new BeanNameGenerator()).run(args);
    }

}
