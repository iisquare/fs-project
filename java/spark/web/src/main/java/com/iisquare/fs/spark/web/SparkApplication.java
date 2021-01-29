package com.iisquare.fs.spark.web;

import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "com.iisquare.fs.base.core.*",
        "com.iisquare.fs.base.jpa.*",
        "com.iisquare.fs.web.core.*",
        "com.iisquare.fs.spark.web",
})
@EnableFeignClients(basePackages = {
        "com.iisquare.fs.web.core.rpc"
}, defaultConfiguration = { FeignInterceptor.class })
public class SparkApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SparkApplication.class).run(args);
    }

}
