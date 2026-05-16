package com.iisquare.fs.web.file;

import io.minio.MinioClient;
import io.minio.messages.Bucket;
import org.junit.Test;

public class MinIOTests {

    final static String MINIO_URL = "";
    final static String MINIO_ACCESS = "";
    final static String MINIO_SECRET = "";

    @Test
    public void clientTest() throws Exception {
        MinioClient client = MinioClient.builder()
                .endpoint(MINIO_URL).credentials(MINIO_ACCESS, MINIO_SECRET).build();
        for (Bucket bucket : client.listBuckets()) {
            System.out.println(bucket.name());
        }
        client.close();
    }

}
