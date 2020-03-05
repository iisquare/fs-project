package com.iisquare.fs.web.admin;

import com.iisquare.fs.base.web.mvc.BeanNameGenerator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.iisquare.fs.base.core.*",
    "com.iisquare.fs.base.jpa.*",
    "com.iisquare.fs.web.admin",
})
@EnableFeignClients
public class ManageApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ManageApplication.class).beanNameGenerator(new BeanNameGenerator()).run(args);
    }
}
