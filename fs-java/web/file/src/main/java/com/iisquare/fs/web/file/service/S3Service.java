package com.iisquare.fs.web.file.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class S3Service {

    private static final Pattern NON_ASCII = Pattern.compile("[^\\x20-\\x7E]");

    @Autowired
    S3Client client;

    public Map<String, Object> listBuckets() {
        try {
            ArrayNode rows = DPUtil.arrayNode();
            for (Bucket bucket : client.listBuckets().buckets()) {
                ObjectNode row = rows.addObject();
                row.put("name", bucket.name());
                if (null != bucket.creationDate()) {
                    row.put("creationDate", bucket.creationDate().toString());
                }
            }
            return ApiUtil.result(0, null, rows);
        } catch (Exception e) {
            return ApiUtil.result(1500, "获取桶信息失败", e.getMessage());
        }
    }

    public Map<String, Object> listObjects(Map<String, Object> params) {
        String bucket = DPUtil.parseString(params.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.result(1001, "桶不能为空", bucket);
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(bucket);
        String startAfter = DPUtil.parseString(params.get("startAfter"));
        if (!DPUtil.empty(startAfter)) builder.startAfter(startAfter);
        String prefix = DPUtil.parseString(params.get("prefix"));
        if (!DPUtil.empty(prefix)) builder.prefix(prefix);
        int limit = Math.min(Math.max(DPUtil.parseInt(params.get("limit")), 1), 500);
        builder.maxKeys(limit);
        ObjectNode data = DPUtil.objectNode();
        data.put("pageSize", limit);
        ArrayNode rows = data.putArray("rows");
        try {
            ListObjectsV2Response response = client.listObjectsV2(builder.build());
            for (S3Object item : response.contents()) {
                ObjectNode row = rows.addObject();
                row.put("objectName", item.key());
                row.put("size", null == item.size() ? 0L : item.size());
                if (null != item.lastModified()) {
                    row.put("lastModified", item.lastModified().toString());
                }
                if (!DPUtil.empty(item.eTag())) row.put("etag", item.eTag());
                if (!DPUtil.empty(item.storageClassAsString())) row.put("storageClass", item.storageClassAsString());
            }
            data.put("isTruncated", Boolean.TRUE.equals(response.isTruncated()));
            if (!DPUtil.empty(response.nextContinuationToken())) {
                data.put("nextContinuationToken", response.nextContinuationToken());
            }
        } catch (Exception e) {
            return ApiUtil.result(1500, "获取对象列表失败", e.getMessage());
        }
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> composeObject(Map<String, Object> params) {
        String bucket = DPUtil.parseString(params.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.result(1001, "桶不能为空", bucket);
        List<String> sources = new ArrayList<>();
        for (String name : DPUtil.parseStringList(params.get("sources"))) {
            if (DPUtil.empty(name)) continue;
            sources.add(name);
        }
        if (sources.isEmpty()) return ApiUtil.result(1002, "源对象不能为空", sources);
        String target = DPUtil.parseString(params.get("target"));
        if (DPUtil.empty(target)) return ApiUtil.result(1003, "目标对象不能为空", target);
        Map<String, String> metadata = null;
        Object metadataValue = params.get("metadata");
        if (metadataValue instanceof Map) {
            metadata = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) metadataValue).entrySet()) {
                if (null == entry.getKey() || null == entry.getValue()) continue;
                metadata.put(DPUtil.parseString(entry.getKey()), DPUtil.parseString(entry.getValue()));
            }
        } else if (null != metadataValue) {
            return ApiUtil.result(1004, "获取元数据失败", metadataValue);
        }
        CreateMultipartUploadRequest.Builder create = CreateMultipartUploadRequest.builder();
        create.bucket(bucket).key(target);
        if (null != metadata) create.metadata(asciiMetadata(metadata));
        String uploadId = null;
        try {
            uploadId = client.createMultipartUpload(create.build()).uploadId();
            List<CompletedPart> parts = new ArrayList<>();
            int partNumber = 1;
            for (String source : sources) {
                UploadPartCopyResponse response = client.uploadPartCopy(UploadPartCopyRequest.builder()
                        .destinationBucket(bucket).destinationKey(target)
                        .sourceBucket(bucket).sourceKey(source)
                        .uploadId(uploadId).partNumber(partNumber).build());
                parts.add(CompletedPart.builder().partNumber(partNumber)
                        .eTag(response.copyPartResult().eTag()).build());
                partNumber++;
            }
            CompleteMultipartUploadResponse response = client.completeMultipartUpload(
                    CompleteMultipartUploadRequest.builder()
                            .bucket(bucket).key(target).uploadId(uploadId)
                            .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                            .build());
            ObjectNode data = DPUtil.objectNode();
            data.put("bucket", response.bucket());
            data.put("object", response.key());
            data.put("etag", response.eTag());
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            if (null != uploadId) {
                try {
                    client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                            .bucket(bucket).key(target).uploadId(uploadId).build());
                } catch (Exception ignored) {}
            }
            return ApiUtil.result(1500, "合并对象失败", e.getMessage());
        }
    }

    public HeadObjectResponse statObject(String bucket, String object) throws Exception {
        return client.headObject(HeadObjectRequest.builder().bucket(bucket).key(object).build());
    }

    public boolean exists(String bucket, String object) throws Exception {
        try {
            statObject(bucket, object);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (404 == e.statusCode()) return false;
            throw e;
        }
    }

    public ResponseInputStream<GetObjectResponse> getObject(String bucket, String object) throws Exception {
        return client.getObject(GetObjectRequest.builder().bucket(bucket).key(object).build());
    }

    public PutObjectResponse putObject(String bucket, String object, InputStream stream, long objectSize,
                                       String contentType, Map<String, String> metadata) throws Exception {
        PutObjectRequest.Builder builder = PutObjectRequest.builder().bucket(bucket).key(object);
        if (!DPUtil.empty(contentType)) builder.contentType(contentType);
        if (null != metadata) builder.metadata(asciiMetadata(metadata));
        RequestBody body = objectSize >= 0
                ? RequestBody.fromInputStream(stream, objectSize)
                : RequestBody.fromBytes(stream.readAllBytes());
        return client.putObject(builder.build(), body);
    }

    private Map<String, String> asciiMetadata(Map<String, String> metadata) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (DPUtil.empty(name) || null == value) continue;
            result.put(name, NON_ASCII.matcher(value).find()
                    ? URLEncoder.encode(value, StandardCharsets.UTF_8) : value);
        }
        return result;
    }

    public Map<String, Object> removeObject(Map<String, Object> params) {
        String bucket = DPUtil.parseString(params.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.result(1001, "桶不能为空", bucket);
        String object = DPUtil.parseString(params.get("object"));
        if (DPUtil.empty(object)) return ApiUtil.result(1002, "删除对象不能为空", object);
        DeleteObjectRequest.Builder builder = DeleteObjectRequest.builder().bucket(bucket).key(object);
        String versionId = DPUtil.parseString(params.get("versionId"));
        if (!DPUtil.empty(versionId)) builder.versionId(versionId);
        if (params.containsKey("bypassRetentionMode")) {
            builder.bypassGovernanceRetention(DPUtil.parseBoolean(params.get("bypassRetentionMode")));
        }
        try {
            client.deleteObject(builder.build());
            return ApiUtil.result(0, null, objectState(bucket, object, true));
        } catch (NoSuchKeyException e) {
            return ApiUtil.result(0, null, objectState(bucket, object, false));
        } catch (NoSuchBucketException e) {
            return ApiUtil.result(1500, "存储桶不存在", e.getMessage());
        } catch (S3Exception e) {
            String errorCode = null == e.awsErrorDetails() ? null : e.awsErrorDetails().errorCode();
            if (404 == e.statusCode() && !"NoSuchBucket".equals(errorCode)) {
                return ApiUtil.result(0, null, objectState(bucket, object, false));
            }
            return ApiUtil.result(1500, "删除对象失败", e.getMessage());
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除对象失败", e.getMessage());
        }
    }

    private ObjectNode objectState(String bucket, String object, boolean deleted) {
        ObjectNode data = DPUtil.objectNode();
        data.put("bucket", bucket);
        data.put("object", object);
        data.put("deleted", deleted);
        return data;
    }

    public Map<String, Object> removeObjects(Map<String, Object> params) {
        String bucket = DPUtil.parseString(params.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.result(1001, "桶不能为空", bucket);
        List<ObjectIdentifier> objects = new LinkedList<>();
        for (String object : DPUtil.parseStringList(params.get("objects"))) {
            if (DPUtil.empty(object)) continue;
            objects.add(ObjectIdentifier.builder().key(object).build());
        }
        if (objects.isEmpty()) return ApiUtil.result(1002, "删除对象不能为空", objects);
        ObjectNode data = DPUtil.objectNode();
        try {
            DeleteObjectsResponse response = client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucket).delete(Delete.builder().objects(objects).build()).build());
            for (S3Error error : response.errors()) {
                ObjectNode item = data.putObject(error.key());
                item.put("code", error.code());
                item.put("message", error.message());
            }
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除文件失败", e.getMessage());
        }
        return ApiUtil.result(0, null, data);
    }

}
