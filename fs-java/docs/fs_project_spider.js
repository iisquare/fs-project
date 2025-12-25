/**
 * 泛采集
 */
db.getCollection("fs_spider_html").drop();
db.createCollection("fs_spider_html");
db.getCollection("fs_spider_html").createIndex({
    content_type: 1
}, {
    name: "idx_content_type"
});
db.getCollection("fs_spider_html").createIndex({
    updated_time: 1
}, {
    name: "idx_updated_time"
});
db.getCollection("fs_spider_html").createIndex({
    domain: 1
}, {
    name: "idx_domain"
});
db.getCollection("fs_spider_html").createIndex({
    response_status: 1
}, {
    name: "idx_response_status"
});
