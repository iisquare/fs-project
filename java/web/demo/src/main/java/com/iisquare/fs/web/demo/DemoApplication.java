package com.iisquare.fs.web.demo;

import com.iisquare.fs.base.web.mvc.BeanNameGenerator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.iisquare.fs.web.demo.entity"})
@EnableJpaRepositories(basePackages = {"com.iisquare.fs.web.demo.dao"})
@EnableFeignClients(basePackages = {"com.iisquare.fs.web.demo.rpc"})
public class DemoApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class).beanNameGenerator(new BeanNameGenerator()).run(args);
    }

}
