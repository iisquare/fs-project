# neo4j

图数据库

## 如何使用

### 基础概念

- 节点可以设置多个标签，关系仅能设置一个标签。
- 索引可应用于节点和关系，唯一约束仅应用于节点。

### 常用命令

- 查看结构
```
:schema
```
- 索引
```
CREATE INDEX {INDEX} IF NOT EXISTS FOR (n:{LABEL}) ON (n.{PROPERTY});
CREATE INDEX {INDEX} IF NOT EXISTS FOR ()-[r:{LABEL}]-() ON (r.{PROPERTY});
DROP INDEX {INDEX};
```
- 约束
```
CREATE CONSTRAINT {CONSTRAINT} IF NOT EXISTS FOR (n:{LABEL}) REQUIRE n.{PROPERTY} IS UNIQUE;
DROP CONSTRAINT {CONSTRAINT};
```
- 节点
```
CREATE (n:{LABEL1}:{LABEL2} { K1:V1, K2:V2 }) RETURN n;
MATCH (n) WHERE id(n) IN [{ID1}, {ID2}, {ID3}] DETACH DELETE n RETURN COUNT(n);
MERGE (n:{LABEL} {PROPERTY}) ON CREATE SET n.k1=v1, n.k2=v2 ON MATCH SET n.k=v RETURN n;
MATCH (p:Person {name: 'Jennifer'}) SET p.birthdate = date('1980-01-01') RETURN p;
MATCH (n:Person {name: 'Jennifer'}) REMOVE n.birthdate;
MATCH (n:Person {name: 'Jennifer'}) SET n.birthdate = null;
```
- 关系
```
MATCH (a:{LABEL}), (b:{LABEL}) WHERE {FILTER} CREATE (a)-[r:{LABEL} {PROPERTY}]->(b) RETURN r;
MATCH ()-[r]->() WHERE id(r) IN [{ID1}, {ID2}, {ID3}] DELETE r RETURN COUNT(r);
MATCH (a:{LABEL}), (b:{LABEL}) MERGE (a)-[r:{LABEL} {PROPERTY}]->(b) RETURN r;
MATCH (:Person {name: 'Jennifer'})-[rel:WORKS_FOR]-(:Company {name: 'Neo4j'}) SET rel.startYear = date({year: 2018}) RETURN rel;
MATCH (p:Person {name: 'Jennifer'})-[rel:WORKS_FOR]-(c:Company {name: 'Neo4j'}) SET rel.startYear = date({year: 2018}) RETURN p, rel, c;
```
- 备份还原
```
neo4j-admin dump --database=neo4j --to=/path/to/backup.dump
neo4j-admin load --from=/path/to/backup.dump --database=neo4j --force
sudo docker-compose stop neo4j # 停止数据库
sudo docker run -it -v /data/runtime/neo4j/data:/data -v /data/runtime/neo4j/logs:/logs --entrypoint /var/lib/neo4j/bin/neo4j-admin docker_neo4j:latest load --from=/logs/graph.db.dump --database=neo4j --force
```
- 导入
```
LOAD CSV FROM "file:///path/to/file.csv" AS line {statement line[0], line[1]};
LOAD CSV WITH HEADERS FROM "file:///path/to/file.csv" AS line {statement line.xxx, line.xxx};
```

## 参考
- [Using Neo4j from Java](https://neo4j.com/developer/java/)
- [The Neo4j Java Driver Manual v4.4](https://neo4j.com/docs/java-manual/current/)
