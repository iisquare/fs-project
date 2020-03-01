package com.iisquare.fs.web.xlab;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.iisquare.fs.base.core.*",
    "com.iisquare.fs.base.swagger.*",
    "com.iisquare.fs.web.xlab"
})
@EnableFeignClients
public class XlabApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(XlabApplication.class).run(args);
    }

}
