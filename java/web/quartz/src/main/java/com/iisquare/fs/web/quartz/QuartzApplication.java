package com.iisquare.fs.web.quartz;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.iisquare.fs.base.core.*",
    "com.iisquare.fs.base.swagger.*",
    "com.iisquare.fs.base.jpa.*",
    "com.iisquare.fs.web.quartz"
})
@EnableFeignClients
@EnableScheduling
public class QuartzApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(QuartzApplication.class).run(args);
    }
}
