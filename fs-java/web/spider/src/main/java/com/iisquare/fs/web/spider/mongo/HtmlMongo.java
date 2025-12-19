package com.iisquare.fs.web.spider.mongo;

import com.iisquare.fs.base.mongodb.mvc.MongoBase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class HtmlMongo extends MongoBase {

    static ThreadLocal<String> tables = new ThreadLocal<>();

    public HtmlMongo() {
        this.database = "fs_project";
        this.table = "fs_spider_html";
    }

    public HtmlMongo switchTable(String table) {
        tables.set(table);
        return this;
    }

    @Override
    public MongoCollection<Document> collection() {
        String table = tables.get();
        if (null == table) {
            table = this.table;
        }
        return super.collection(table);
    }

    @Override
    public Document filtration(Document document) {
        Document info = new Document();
        if (document.containsKey(FIELD_ID)) { // 任务链接
            info.put(FIELD_ID, document.get(FIELD_ID));
        }
        if (document.containsKey("job_id")) { // 作业标识
            info.put("job_id", document.getString("job_id"));
        }
        if (document.containsKey("url")) { // 实际响应链接，任务链接或301跳转后的真实链接
            info.put("url", document.getString("url"));
        }
        if (document.containsKey("type")) { // 判定类型
            info.put("type", document.getString("type"));
        }
        if (document.containsKey("content_type")) { // 页面类型
            info.put("content_type", document.getString("content_type"));
        }
        if (document.containsKey("domain")) { // 域名
            info.put("domain", document.getString("domain"));
        }
        if (document.containsKey("uri")) { // 路径
            info.put("uri", document.getString("uri"));
        }
        if (document.containsKey("uris")) { // 拆分后的路径数组
            info.put("uris", document.getList("uris", String.class));
        }
        if (document.containsKey("title")) {
            info.put("title", document.getString("title"));
        }
        if (document.containsKey("keywords")) {
            info.put("keywords", document.getList("keywords", String.class));
        }
        if (document.containsKey("description")) {
            info.put("description", document.getString("description"));
        }
        if (document.containsKey("content")) { // 若为附件则存储文件路径
            info.put("content", document.getString("content"));
        }
        if (document.containsKey("response_status")) { // 页面响应状态
            info.put("response_status", document.getInteger("response_status"));
        }
        if (document.containsKey("response_time")) { // 响应耗时，单位毫秒
            info.put("response_time", document.getInteger("response_time"));
        }
        if (document.containsKey("exception")) { // 异常信息
            info.put("exception", document.getString("exception"));
        }
        if (document.containsKey("referer")) { // 来源页面链接地址
            info.put("referer", document.getString("referer"));
        }
        if (document.containsKey("created_time")) { // 创建时间
            info.put("created_time", document.getLong("created_time"));
        }
        if (document.containsKey("updated_time")) { // 更新时间
            info.put("updated_time", document.getLong("updated_time"));
        }
        if (document.containsKey("deleted_time")) { // 删除时间
            info.put("deleted_time", document.getLong("deleted_time"));
        }
        return info;
    }

}
