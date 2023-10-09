# Kafka

## 使用说明

### 常用命令

- 创建主题
```
kafka-topics.sh --bootstrap-server kafka:9092 --create --topic <topic_name>  --partitions 1 --replication-factor 1
```
- 查看主题
```
kafka-topics.sh --bootstrap-server kafka:9092 --list
```
- 主题详情
```
kafka-topics.sh --bootstrap-server kafka:9092 --describe --topic <topic_name>
```
- 修改主题
```
kafka-configs.sh --zookeeper zookeeper:2181 --entity-type topics --entity-name <topic_name> --alter --add-config max.message.bytes=10485760
kafka-topics.sh --bootstrap-server kafka:9092 --alter --topic <topic_name> --partitions 1 --config retention.ms=86400000 --config cleanup.policy=delete
```
- 删除主题
```
kafka-topics.sh --bootstrap-server kafka:9092 --delete --topic <topic_name>
```

- 生产消息
```
kafka-console-producer.sh --broker-list kafka:9092 --topic <topic_name>
```
- 消费消息
```
kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic <topic_name> --from-beginning
```
- 删除消息
```
kafka-delete-records.sh --bootstrap-server kafka:9092--offset-json-file offset.json
{
    "partitions": [{
        "topic": "fs_test",
        "partition": 1,
        "offset": 10 # 为-1时表示删除所有数据
    }],
    "version": 1
}
```

- 查看分组
```
kafka-consumer-groups.sh --bootstrap-server kafka:9092 --list
```
- 分组详情
```
kafka-consumer-groups.sh --bootstrap-server kafka:9092 --describe --group <group_name>
```
- 删除分组
```
kafka-consumer-groups.sh --bootstrap-server kafka:9092 --delete --group <group_name>
```
- 重置偏移
```
–-all-topics 所有主题
–-topic t1 --topic t2 指定多个主题
–-topic t1:0,1,2 指定主题分区
–-to-earliest
--to-latest
--to-offset N
--to-current
--shift-by N 步减
--to-datetime 2019-03-03T03:30:00.000
--by-duration PT0H30M0S 调整为30分组之前的最早偏移
kafka-consumer-groups.sh --bootstrap-server kafka:9092 --group <group_name> --reset-offsets --all-topics --to-earliest --execute
```


## 最佳实践

### 配置参数
```
log.retention.ms/log.retention.minutes/log.retention.ms：消息保留时长，以最小值的参数为准。
log.retention.bytes：每一个分区下的数据保留大小。
log.segment.bytes：日志片段大小，到达上限后关闭旧分段，打开新分段。
log.segment.ms：日志片段时间，日志片段会在大小或时间达到上限时被关闭。
message.max.bytes：单条消息大小（压缩后），默认1000000（1MB）。
```


## 注意事项

### KAFKA_LISTENERS

- PLAINTEXT://:9092，默认采用主机名，确保客户端存在主机名到IP地址的映射。
- PLAINTEXT://[ip]:9092，采用固定IP访问。
- PLAINTEXT://[hostname]:9092，采用域名解析，确保客户端可正常解析对应域名。

```
Caused by: org.apache.kafka.common.errors.TimeoutException: Timeout of 60000ms expired before the position for partition fs_test-0 could be determined
```
