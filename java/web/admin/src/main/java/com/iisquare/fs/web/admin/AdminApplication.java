package com.iisquare.fs.web.admin;

import com.iisquare.fs.base.web.mvc.BeanNameGenerator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.iisquare.fs.base.core.*",
        "com.iisquare.fs.web.admin",
})
@EnableFeignClients(basePackages = {
        "com.iisquare.fs.web.core.rpc"
})
public class AdminApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(AdminApplication.class).beanNameGenerator(new BeanNameGenerator()).run(args);
    }

}
