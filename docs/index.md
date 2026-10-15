# 说明文档

存放跨模块的设计说明与方案文档；单个模块的实现说明写在对应模块的 `README.md` 中，此处只做索引。

## 设计文档

| 文档 | 内容 |
| --- | --- |
| [data-sync.md](data-sync.md) | 数据同步：Flink CDC 采集变更、Kafka 日志、HBase 归档、配置中心授权与 ACK 确认流程。 |
| [dw-etl.md](dw-etl.md) | 数据仓库分层（ODS / DWD / DWB / DWS / ADS）的存储选型、初始化与增量处理、预聚合最佳实践。 |

`diagram/` 存放配图与设计器工程文件。

## 相关文档

- [项目说明](../README.md)：项目结构、技术选型、功能模块与快速开始。
- [大数据版本基线](../fs-java/docs/bigdata.md)：Hadoop / Spark / Flink / Flink CDC 版本与生态集成。
- [常见问题](../fs-java/docs/question.md)：Flink 作业提交、JDBC 取值异常等问题及解决方案。
- [待办事项](../fs-java/docs/future.md)：分析层、报表层、计算优化与遗留问题。
- `fs-java/web/<模块>/README.md` 与 `fs-admin/README.md`：模块级功能、接口与目录约定。

## 工具

### DBeaver

- 工具转储数据库每条记录一行

```
额外的命令行参数：--skip-extended-insert
```
