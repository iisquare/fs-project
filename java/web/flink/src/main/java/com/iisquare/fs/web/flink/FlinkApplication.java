package com.iisquare.fs.web.flink;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {ElasticsearchAutoConfiguration.class, RestClientAutoConfiguration.class})
@ComponentScan(basePackages = {
    "com.iisquare.fs.base.core.*",
    "com.iisquare.fs.base.jpa.*",
    "com.iisquare.fs.base.swagger.*",
    "com.iisquare.fs.web.flink"
})
public class FlinkApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(FlinkApplication.class).run(args);
    }

}
