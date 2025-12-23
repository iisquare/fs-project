/**
 * 泛采集
 */
db.getCollection("fs_spider_html").drop();
db.createCollection("fs_spider_html");
db.getCollection("fs_spider_html").createIndex({
    frameId: NumberInt("1")
}, {
    name: "idx_frame_id"
});
db.getCollection("fs_spider_html").createIndex({
    content_type: NumberInt("1")
}, {
    name: "idx_content_type"
});
db.getCollection("fs_spider_html").createIndex({
    created_time: NumberInt("1")
}, {
    name: "idx_created_time"
});
db.getCollection("fs_spider_html").createIndex({
    domain: NumberInt("1")
}, {
    name: "idx_domain"
});
db.getCollection("fs_spider_html").createIndex({
    response_status: NumberInt("1")
}, {
    name: "idx_response_status"
});
