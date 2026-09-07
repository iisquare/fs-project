package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.util.HttpClientUtil;
import com.iisquare.fs.web.bi.entity.DataApi;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API接口连接器
 * 配置信息：
 * @see DataApi
 */
public class HttpConnector extends DatasourceConnector<CloseableHttpClient> {

    public HttpConnector(String type, JsonNode config) {
        super(type, config);
    }

    /**
     * 执行接口请求并解析响应结果为JSON，供查询扫描时调用
     */
    public JsonNode execute() throws Exception {
        return execute((Long) null);
    }

    /**
     * 执行接口请求；queryTimeout 为查询级超时（毫秒），优先于数据源自身 timeout 配置。
     */
    public JsonNode execute(Long queryTimeout) throws Exception {
        return execute(queryTimeout, -1L, -1L);
    }

    /**
     * 按页码和分页大小执行接口请求。
     */
    public JsonNode execute(long page, long pageSize) throws Exception {
        return execute(null, page, pageSize);
    }

    /**
     * 执行接口请求；page、pageSize 大于 0 时，将按照配置的分页字段写入请求参数。
     */
    public JsonNode execute(Long queryTimeout, long page, long pageSize) throws Exception {
        String url = DPUtil.trim(config.at("/url").asText());
        String method = DPUtil.trim(config.at("/method").asText());
        int timeout = config.at("/timeout").asInt(0);
        if (timeout <= 0) timeout = 3000; // 默认超时时间
        if (null != queryTimeout && queryTimeout > 0) {
            timeout = queryTimeout > Integer.MAX_VALUE ? Integer.MAX_VALUE : queryTimeout.intValue();
        }
        String contentType = DPUtil.trim(config.at("/contentType").asText());
        CloseableHttpClient client = null;
        try {
            client = open();
            HttpRequestBase request = request(url, method, timeout, config.at("/headers"),
                    contentType, config.at("/payloadForm"), config.at("/payloadBody").asText(),
                    page, pageSize);
            try (CloseableHttpResponse response = client.execute(request)) {
                int code = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String body = null == entity ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
                if (code < 200 || code >= 300) {
                    throw new IOException("HTTP " + code + " - " + body);
                }
                JsonNode json = DPUtil.empty(body) ? DPUtil.objectNode() : DPUtil.parseJSON(body);
                if (null == json) throw new IOException("响应结果解析失败: " + body);
                return json;
            }
        } finally {
            close(client);
        }
    }

    @Override
    public CloseableHttpClient open() throws Exception {
        PoolingHttpClientConnectionManager pooling = new PoolingHttpClientConnectionManager();
        pooling.setMaxTotal(200); // 最大连接数
        pooling.setDefaultMaxPerRoute(100); // 默认的每个路由的最大连接数

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionManager(pooling);
        return builder.build();
    }

    @Override
    public void close(CloseableHttpClient client) {
        FileUtil.close(client);
    }

    @Override
    public Map<String, Object> test() {
        String url = DPUtil.trim(config.at("/url").asText());
        if (DPUtil.empty(url)) return ApiUtil.result(1001, "接口地址不能为空", url);
        String method = DPUtil.trim(config.at("/method").asText());
        int timeout = config.at("/timeout").asInt(0);
        if (timeout <= 0) timeout = 3000; // 默认超时时间
        String contentType = DPUtil.trim(config.at("/contentType").asText());
        CloseableHttpClient client = null;
        try {
            client = open();
            HttpRequestBase request = request(url, method, timeout, config.at("/headers"),
                    contentType, config.at("/payloadForm"), config.at("/payloadBody").asText(),
                    -1L, -1L);
            try (CloseableHttpResponse response = client.execute(request)) {
                int code = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String body = null == entity ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
                if (code < 200 || code >= 300) {
                    return ApiUtil.result(1500, "连接失败", "HTTP " + code + " - " + body);
                }
                JsonNode json = DPUtil.empty(body) ? DPUtil.objectNode() : DPUtil.parseJSON(body);
                if (null == json) return ApiUtil.result(1500, "响应结果解析失败", body);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", code);
                data.put("headers", HttpClientUtil.headers(response));
                data.put("schema", schema(json));
                data.put("json", json);
                return ApiUtil.result(0, "连接成功", data);
            }
        } catch (Exception e) {
            return ApiUtil.result(1500, "连接失败", e.getMessage());
        } finally {
            close(client);
        }
    }

    /**
     * 构建请求对象，GET请求参数拼接至地址栏，POST请求参数放置于请求体。
     * 分页参数优先写入表单或JSON请求体，无法写入时回退为地址栏查询参数。
     */
    private HttpRequestBase request(String url, String method, int timeout, JsonNode headers,
                                    String contentType, JsonNode payloadForm, String payloadBody,
                                    long page, long pageSize) throws Exception {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout).setSocketTimeout(timeout).setConnectionRequestTimeout(timeout).build();
        boolean paged = paged(page, pageSize);
        JsonNode form = pagedPayloadForm(payloadForm, page, pageSize);
        String body = pagedBody(contentType, payloadBody, page, pageSize);
        String targetUrl = url;
        HttpRequestBase request;
        if ("get".equalsIgnoreCase(method)) {
            request = new HttpGet(buildUrl(url, form));
        } else {
            boolean formOrJsonBody = "form-data".equals(contentType)
                    || "x-www-form-urlencoded".equals(contentType)
                    || ("json".equals(contentType) && injectableJsonBody(payloadBody));
            if (paged && !formOrJsonBody) {
                targetUrl = buildUrl(url, form);
            }
            HttpPost post = new HttpPost(targetUrl);
            HttpEntity entity = buildEntity(contentType, form, body);
            if (null != entity) post.setEntity(entity);
            request = post;
        }
        request.setConfig(config);
        fillHeaders(request, headers); // 自定义请求头优先，已包含Content-Type时不覆盖
        return request;
    }

    private boolean paged(long page, long pageSize) {
        if (page <= 0 && pageSize <= 0) return false;
        return !DPUtil.empty(config.at("/pageRequestField").asText())
                || !DPUtil.empty(config.at("/pageSizeRequestField").asText());
    }

    private JsonNode pagedPayloadForm(JsonNode payloadForm, long page, long pageSize) {
        ObjectNode form = null != payloadForm && payloadForm.isObject()
                ? (ObjectNode) payloadForm.deepCopy() : DPUtil.objectNode();
        if (paged(page, pageSize)) {
            applyPagination(form, page, pageSize);
        }
        return form;
    }

    private String pagedBody(String contentType, String payloadBody, long page, long pageSize) {
        if (!"json".equals(contentType) || !paged(page, pageSize) || !injectableJsonBody(payloadBody)) {
            return payloadBody;
        }
        JsonNode json = DPUtil.empty(payloadBody) ? DPUtil.objectNode() : DPUtil.parseJSON(payloadBody);
        if (null == json || !json.isObject()) return payloadBody;
        ObjectNode body = (ObjectNode) json.deepCopy();
        applyPagination(body, page, pageSize);
        return DPUtil.stringify(body);
    }

    private boolean injectableJsonBody(String payloadBody) {
        if (DPUtil.empty(payloadBody)) return true;
        JsonNode json = DPUtil.parseJSON(payloadBody);
        return null != json && json.isObject();
    }

    private void applyPagination(ObjectNode target, long page, long pageSize) {
        String pageField = DPUtil.trim(config.at("/pageRequestField").asText());
        String pageSizeField = DPUtil.trim(config.at("/pageSizeRequestField").asText());
        if (!DPUtil.empty(pageField) && page > 0) {
            setPath(target, pageField, DPUtil.toJSON(page));
        }
        if (!DPUtil.empty(pageSizeField) && pageSize > 0) {
            setPath(target, pageSizeField, DPUtil.toJSON(pageSize));
        }
    }

    private static void setPath(ObjectNode root, String path, JsonNode value) {
        if (null == root || DPUtil.empty(path)) return;
        String[] segments = path.split("\\.");
        ObjectNode current = root;
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            JsonNode child = current.get(segment);
            if (null == child || !child.isObject()) {
                child = DPUtil.objectNode();
                current.set(segment, child);
            }
            current = (ObjectNode) child;
        }
        current.set(segments[segments.length - 1], value);
    }

    public long total(JsonNode response) {
        return longValue(response, config.at("/totalResponseField").asText(""));
    }

    public long page(JsonNode response) {
        return longValue(response, config.at("/pageResponseField").asText(""));
    }

    public long pageSize(JsonNode response) {
        return longValue(response, config.at("/pageSizeResponseField").asText(""));
    }

    /**
     * 未配置记录总数时，根据返回分页大小推断是否还有下一页。
     */
    public boolean hasMore(JsonNode response, int returnedRows, int requestedPageSize) {
        long pageSize = pageSize(response);
        if (pageSize <= 0) pageSize = requestedPageSize;
        return pageSize > 0 && returnedRows >= pageSize;
    }

    private long longValue(JsonNode response, String path) {
        if (null == response || DPUtil.empty(path)) return -1L;
        JsonNode node = resolve(response, path);
        if (null == node || node.isNull()) return -1L;
        return node.isNumber() ? node.asLong() : DPUtil.parseLong(node.asText(), -1L);
    }

    private static JsonNode resolve(JsonNode root, String path) {
        if (path == null || path.isEmpty()) return root;
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (current == null || current.isNull()) return null;
            if (current.isArray()) {
                try {
                    current = current.get(Integer.parseInt(segment));
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                current = current.get(segment);
            }
        }
        return current;
    }

    /**
     * 构建请求体，none或不支持的类型不携带请求体
     */
    private HttpEntity buildEntity(String contentType, JsonNode payloadForm, String payloadBody) throws Exception {
        switch (contentType) {
            case "form-data": { // 自定义multipart/form-data请求体
                String boundary = "----fs-bi-" + System.currentTimeMillis();
                StringBuilder body = new StringBuilder();
                if (null != payloadForm && payloadForm.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> iterator = payloadForm.fields();
                    while (iterator.hasNext()) {
                        Map.Entry<String, JsonNode> entry = iterator.next();
                        body.append("--").append(boundary).append("\r\n");
                        body.append("Content-Disposition: form-data; name=\"")
                                .append(entry.getKey()).append("\"\r\n\r\n");
                        body.append(entry.getValue().asText()).append("\r\n");
                    }
                }
                body.append("--").append(boundary).append("--\r\n");
                StringEntity entity = new StringEntity(body.toString(), StandardCharsets.UTF_8);
                entity.setContentType("multipart/form-data; boundary=" + boundary);
                return entity;
            }
            case "x-www-form-urlencoded": { // 键值对格式请求体
                StringEntity entity = new StringEntity(buildQueryString(payloadForm), StandardCharsets.UTF_8);
                entity.setContentType(defaultContentType(contentType));
                return entity;
            }
            case "json":
            case "xml":
            case "raw": {
                StringEntity entity = new StringEntity(null == payloadBody ? "" : payloadBody, StandardCharsets.UTF_8);
                entity.setContentType(defaultContentType(contentType));
                return entity;
            }
            default:
                return null;
        }
    }

    private void fillHeaders(HttpRequestBase request, JsonNode headers) {
        if (null == headers || !headers.isObject()) return;
        for (Map.Entry<String, JsonNode> entry : headers.properties()) {
            request.setHeader(entry.getKey(), entry.getValue().asText());
        }
    }

    private String defaultContentType(String contentType) {
        return switch (contentType) {
            case "json" -> "application/json; charset=UTF-8";
            case "xml" -> "application/xml; charset=UTF-8";
            case "raw" -> "text/plain; charset=UTF-8";
            default -> "application/x-www-form-urlencoded; charset=UTF-8";
        };
    }

    private String buildUrl(String url, JsonNode payloadForm) throws Exception {
        String queryString = buildQueryString(payloadForm);
        if (DPUtil.empty(queryString)) return url;
        return url + (url.contains("?") ? "&" : "?") + queryString;
    }

    private String buildQueryString(JsonNode payloadForm) throws Exception {
        List<String> list = new ArrayList<>();
        if (null != payloadForm && payloadForm.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = payloadForm.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String value = entry.getValue().asText();
                if (DPUtil.empty(value)) {
                    list.add(entry.getKey() + "=");
                } else {
                    list.add(entry.getKey() + "=" + URLEncoder.encode(value, "UTF-8"));
                }
            }
        }
        return DPUtil.implode("&", list.toArray(new String[0]));
    }

    /**
     * 根据响应结果生成字段结构，数组或对象的下级使用.点分割，数组中的对象作为下级
     */
    private List<Map<String, Object>> schema(JsonNode json) {
        List<Map<String, Object>> schema = new ArrayList<>();
        if (null == json || json.isNull()) return schema;
        if (json.isArray()) {
            schema.add(schema("", json)); // 顶层为数组时，以数组元素作为下级
        } else if (json.isObject()) {
            for (Map.Entry<String, JsonNode> entry : json.properties()) {
                schema.add(schema(entry.getKey(), entry.getValue()));
            }
        }
        return schema;
    }

    private Map<String, Object> schema(String path, JsonNode json) {
        Map<String, Object> node = new LinkedHashMap<>();
        String field = path.substring(path.lastIndexOf('.') + 1); // 当前级别的字段名称
        node.put("path", path); // 请求结果的字段路径，数组或对象的下级使用.点分割
        node.put("field", field);
        node.put("name", field);
        node.put("title", "");
        node.put("type", typeOf(json));
        node.put("comment", "");
        node.put("checked", false);
        List<Map<String, Object>> children = new ArrayList<>();
        if (json.isObject()) {
            for (Map.Entry<String, JsonNode> entry : json.properties()) {
                children.add(schema(join(path, entry.getKey()), entry.getValue()));
            }
        } else if (json.isArray() && !json.isEmpty()) {
            JsonNode first = json.get(0);
            if (first.isObject()) { // 数组中的对象作为下级
                for (Map.Entry<String, JsonNode> entry : first.properties()) {
                    children.add(schema(join(path, entry.getKey()), entry.getValue()));
                }
            }
        }
        node.put("children", children);
        return node;
    }

    private String join(String prefix, String name) {
        return DPUtil.empty(prefix) ? name : prefix + "." + name;
    }

    private String typeOf(JsonNode json) {
        if (json.isObject()) return "object";
        if (json.isArray()) return "array";
        if (json.isTextual()) return "string";
        if (json.isNumber()) return "number";
        if (json.isBoolean()) return "boolean";
        return "";
    }

}
