package com.iisquare.fs.base.minio.config;

import com.iisquare.fs.base.core.util.FileUtil;
import io.minio.MinioClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfiguration implements DisposableBean {

    @Value("${spring.minio.endpoint}")
    private String endpoint;
    @Value("${spring.minio.accessKey}")
    private String accessKey;
    @Value("${spring.minio.secretKet}")
    private String secretKet;

    MinioClient client;

    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    @Bean
    public MinioClient minioClient() throws Exception {
        MinioClient.Builder builder = MinioClient.builder();
        builder.endpoint(endpoint);
        builder.credentials(accessKey, secretKet);
        return this.client = builder.build();
    }

}
