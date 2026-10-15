package com.iisquare.fs.web.file;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.net.URI;

public class S3Tests {

    final static String S3_URL = "";
    final static String S3_ACCESS = "";
    final static String S3_SECRET = "";

    @Test
    public void clientTest() throws Exception {
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(S3_URL))
                .region(Region.of("us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(S3_ACCESS, S3_SECRET)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        for (Bucket bucket : client.listBuckets().buckets()) {
            System.out.println(bucket.name());
        }
        client.close();
    }

}
