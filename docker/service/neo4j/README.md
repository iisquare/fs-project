# Neo4j

## 使用说明

- 默认密码
```
neo4j/neo4j
```
- 禁用认证
```
--env=NEO4J_AUTH=none
```
- 导入导出
```
neo4j-admin dump --database=neo4j --to=/path/to/neo4j.dump
neo4j-admin load --from=/path/to/neo4j.dump --database=neo4j --force
# 容器化处理
sudo docker run -v /data/runtime/neo4j/data:/data --name t-neo4j-dump -it neo4j:4.4.5 /bin/bash
sudo docker cp /host/path/to/neo4j.dump t-neo4j-dump:/
sudo docker rm t-neo4j-dump
```

## 参考

- [Run Neo4j in Docker](https://neo4j.com/developer/docker-run-neo4j/)
