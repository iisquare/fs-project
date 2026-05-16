package com.iisquare.fs.web.file.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * @see(https://min-io.cn/docs/minio/linux/developers/java/API.html)
 */
@Service
public class MinIOService {

    @Autowired
    MinioClient client;

    public Map<String, Object> listBuckets() {
        try {
            List<Bucket> buckets = client.listBuckets();
            return ApiUtil.result(0, null, DPUtil.toJSON(buckets, ObjectNode.class));
        } catch (Exception e) {
            return ApiUtil.result(1500, "获取桶信息失败", e.getMessage());
        }
    }

    public Map<String, Object> listObjects(Map<String, Object> params) {
        ListObjectsArgs.Builder builder = ListObjectsArgs.builder();
        String bucket = DPUtil.parseString(params.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.result(1001, "桶不能为空", bucket);
        builder.bucket(bucket);
        String startAfter = DPUtil.parseString(params.get("startAfter"));
        if (!DPUtil.empty(startAfter)) builder.startAfter(startAfter);
        String prefix = DPUtil.parseString(params.get("prefix"));
        if (!DPUtil.empty(prefix)) builder.prefix(prefix);
        int limit = Math.min(Math.max(DPUtil.parseInt(params.get("limit")), 1), 500);
        builder.maxKeys(limit);
        Iterator<Result<Item>> iterator = client.listObjects(builder.build()).iterator();
        ObjectNode data = DPUtil.objectNode();
        data.put("pageSize", limit);
        ArrayNode rows = data.putArray("rows");
        while (iterator.hasNext()) {
            try {
                Item item = iterator.next().get();
                rows.add(DPUtil.toJSON(item));
            } catch (Exception e) {
                return ApiUtil.result(1500, "获取对象列表失败", e.getMessage());
            }
        }
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> composeObject(Map<String, Object> params) {
        String bucket = DPUtil.parseString(params.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.result(1001, "桶不能为空", bucket);
        List<ComposeSource> sources = new ArrayList<>();
        for (String name : DPUtil.parseStringList(params.get("sources"))) {
            if (DPUtil.empty(name)) continue;
            ComposeSource.Builder builder = ComposeSource.builder();
            builder.bucket(bucket);
            builder.object(name);
            sources.add(builder.build());
        }
        if (sources.isEmpty()) return ApiUtil.result(1002, "源对象不能为空", sources);
        String target = DPUtil.parseString(params.get("target"));
        if (DPUtil.empty(target)) return ApiUtil.result(1003, "目标对象不能为空", target);
        Map<String, String> metadata;
        try {
            metadata = (Map<String, String>) params.get("metadata");
        } catch (Exception e) {
            return ApiUtil.result(1004, "获取元数据失败", e.getMessage());
        }
        ComposeObjectArgs.Builder builder = ComposeObjectArgs.builder();
        builder.bucket(bucket);
        builder.sources(sources);
        builder.object(target);
        if (null != metadata) builder.userMetadata(metadata);
        try {
            ObjectWriteResponse response = client.composeObject(builder.build());
            return ApiUtil.result(0, null, DPUtil.toJSON(response));
        } catch (Exception e) {
            return ApiUtil.result(1500, "合并对象失败", e.getMessage());
        }
    }

    public StatObjectResponse statObject(String bucket, String object) throws Exception {
        StatObjectArgs.Builder builder = StatObjectArgs.builder();
        builder.bucket(bucket);
        builder.object(object);
        return client.statObject(builder.build());
    }

    public GetObjectResponse getObject(String bucket, String object) throws Exception {
        GetObjectArgs.Builder builder = GetObjectArgs.builder();
        builder.bucket(bucket);
        builder.object(object);
        return client.getObject(builder.build());
    }

    public ObjectWriteResponse putObject(String bucket, String object, InputStream stream, String contentType, Map<String, String> headers, Map<String, String> metadata) throws Exception {
        PutObjectArgs.Builder builder = PutObjectArgs.builder();
        builder.bucket(bucket);
        builder.object(object);
        if (!DPUtil.empty(contentType)) builder.contentType(contentType);
        builder.stream(stream, -1, -1);
        if (null != headers) builder.headers(headers);
        if (null != metadata) builder.userMetadata(metadata);
        return client.putObject(builder.build());
    }

    public Map<String, Object> removeObject(Map<String, Object> params) {
        String bucket = DPUtil.parseString(params.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.result(1001, "桶不能为空", bucket);
        String object = DPUtil.parseString(params.get("object"));
        if (DPUtil.empty(object)) return ApiUtil.result(1002, "删除对象不能为空", object);
        RemoveObjectArgs.Builder builder = RemoveObjectArgs.builder();
        builder.bucket(bucket);
        builder.object(object);
        String versionId = DPUtil.parseString(params.get("versionId"));
        if (!DPUtil.empty(versionId)) builder.versionId(versionId);
        if (params.containsKey("bypassRetentionMode")) {
            builder.bypassGovernanceMode(DPUtil.parseBoolean(params.get("bypassRetentionMode")));
        }
        try {
            client.removeObject(builder.build());
            return ApiUtil.result(0, null, object);
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除对象失败", e.getMessage());
        }
    }

    public Map<String, Object> removeObjects(Map<String, Object> params) {
        String bucket = DPUtil.parseString(params.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.result(1001, "桶不能为空", bucket);
        List<DeleteObject> objects = new LinkedList<>();
        for (String object : DPUtil.parseStringList(params.get("objects"))) {
            if (DPUtil.empty(object)) continue;
            objects.add(new DeleteObject(object));
        }
        if (objects.isEmpty()) return ApiUtil.result(1002, "删除对象不能为空", objects);
        RemoveObjectsArgs.Builder builder = RemoveObjectsArgs.builder();
        builder.bucket(bucket);
        builder.objects(objects);
        Iterable<Result<DeleteError>> results = client.removeObjects(builder.build());
        ObjectNode data = DPUtil.objectNode();
        for (Result<DeleteError> result : results) {
            try {
                DeleteError error = result.get();
                data.replace(error.objectName(), DPUtil.toJSON(error));
            } catch (Exception e) {
                return ApiUtil.result(1500, "删除文件失败", e.getMessage());
            }
        }
        return ApiUtil.result(0, null, data);
    }

}
