package com.iisquare.fs.base.s3.config;

import com.iisquare.fs.base.core.util.FileUtil;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3ClientConfiguration implements DisposableBean {

    @Value("${spring.s3.endpoint}")
    private String endpoint;
    @Value("${spring.s3.accessKey}")
    private String accessKey;
    @Value("${spring.s3.secretKey}")
    private String secretKey;
    @Value("${spring.s3.region:us-east-1}")
    private String region;

    S3Client client;

    @Override
    public void destroy() throws Exception {
        FileUtil.close(client);
    }

    @Bean
    public S3Client s3Client() throws Exception {
        S3ClientBuilder builder = S3Client.builder();
        builder.endpointOverride(URI.create(endpoint));
        builder.region(Region.of(region));
        builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)));
        builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        return this.client = builder.build();
    }

}
