# ZooKeeper

## 使用说明

### 执行命令

- CLI客户端
```
sudo docker-compose exec zookeeper zkCli.sh
```
- 日志格式化
```
java -classpath .:slf4j-api-1.7.25.jar:zookeeper-3.5.8.jar  org.apache.zookeeper.server.LogFormatter /datalog/version-2/log.1
```

## 最佳实践

### 日志清理
```
在zoo.cfg配置文件中：
默认将事务日志文件和快照日志文件都存储在dataDir对应的目录下。建议将事务日志（dataLogDir）与快照日志（dataLog）单独配置。
通过autopurge.snapRetainCount指定保留文件数目，默认保留3个。
通过autopurge.purgeInterval指定清理频率（小时），默认0表示不开启自动清理功能。
```
