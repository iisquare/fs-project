package com.iisquare.fs.site.shop.web;

import com.iisquare.fs.base.web.mvc.BeanNameGenerator;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.iisquare.fs.base.core.*",
        "com.iisquare.fs.web.core.*",
        "com.iisquare.fs.site.core.**",
        "com.iisquare.fs.site.shop.web",
})
@EnableFeignClients(basePackages = {
        "com.iisquare.fs.web.core.rpc"
}, defaultConfiguration = { FeignInterceptor.class })
public class ShopWebApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ShopWebApplication.class).beanNameGenerator(new BeanNameGenerator()).run(args);
    }

}
