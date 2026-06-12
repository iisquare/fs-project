package com.iisquare.fs.web.lm;

import com.iisquare.fs.base.web.mvc.BeanNameGenerator;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.iisquare.fs.base.core.*",
        "com.iisquare.fs.base.jpa.*",
        "com.iisquare.fs.web.core.*",
        "com.iisquare.fs.web.lm",
})
@MapperScan("com.iisquare.fs.web.lm.mapper")
@EnableFeignClients(basePackages = {
        "com.iisquare.fs.web.core.rpc"
}, defaultConfiguration = { FeignInterceptor.class })
public class LMApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(LMApplication.class).beanNameGenerator(new BeanNameGenerator()).run(args);
    }

}
