# logstash

### 运行方式
- Filebeat收集宿主机日志并写入到Kafka中
- Logstash读取Kafka中的日志，格式化并写入到Elasticsearch中
- 消费offset通过kafka的group_id进行记录，不再单独持久化logstash的data文件

### 其他事项
- nginx-access-log
```
$remote_addr - $remote_user [$time_local] "$request" $status $body_bytes_sent "$http_referer" "$http_user_agent" $request_length $request_time [$proxy_upstream_name] [$proxy_alternative_upstream_name] $upstream_addr $upstream_response_length $upstream_response_time $upstream_status $req_id
```

### 数据示例
- nginx-access-log
```
192.168.0.139 - - [27/Nov/2020:10:04:38 +0000] "GET /api/v2.0/projects?page=1&page_size=15 HTTP/2.0" 200 760 "https://harbor.iisquare.com/harbor/projects" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.75 Safari/537.36" 50 1.279 [app-lvs-svr-harbor-harbor-core-80] [] 10.244.4.100:8080 760 1.279 200 e0726b2e0c22976ac8ccd04d52d7c83b
```
- mysql-slow-query-log
```
# Time: 2020-11-27T09:59:13.044116Z
# User@Host: root[root] @  [192.168.0.139]  Id:    90
# Query_time: 0.001422  Lock_time: 0.000220 Rows_sent: 15  Rows_examined: 282
SET timestamp=1606471153;
SELECT QUERY_ID, SUM(DURATION) AS SUM_DURATION FROM INFORMATION_SCHEMA.PROFILING GROUP BY QUERY_ID;
```

### 模板管理
- 创建模板
```
PUT _index_template/template_xxx
```
- 删除模板
```
DELETE _index_template/template_xxx
```


### 参考链接
- [NGINX Ingress Controller ConfigMaps](https://kubernetes.github.io/ingress-nginx/user-guide/nginx-configuration/configmap/)
- [Elasticsearch Reference Index templates](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/index-templates.html)
- [Logstash Reference Filter plugins](https://www.elastic.co/guide/en/logstash/7.x/filter-plugins.html)
- [grok-patterns](https://github.com/logstash-plugins/logstash-patterns-core/blob/master/patterns/grok-patterns)
- [Grok Debugger](https://grokdebug.herokuapp.com/)
