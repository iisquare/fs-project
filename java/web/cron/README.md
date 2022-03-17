# 定时任务

## 设计说明

### 任务编排

- 调度流程的基础参数保存在Job.JobDataMap中，流程共享参数保存在Flow.data中。
- 手动触发流程时，Trigger.JobDataMap用于覆盖Flow.data默认参数。
- 一个Flow对应一个Job和一个Trigger，Flow.group=Job.group=Trigger.group，Flow.name=Job.name=Trigger.name。


## 参考链接
- [Azkaban](https://azkaban.readthedocs.io/en/latest/)
- [tables_mysql_innodb.sql](https://github.com/quartz-scheduler/quartz/blob/v2.3.2/quartz-core/src/main/resources/org/quartz/impl/jdbcjobstore/tables_mysql_innodb.sql)
