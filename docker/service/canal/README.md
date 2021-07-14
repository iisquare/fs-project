# Canal

## 使用说明
- 创建canal-admin数据库，导入[canal_manager.sql](https://github.com/alibaba/canal/blob/master/admin/admin-web/src/main/resources/canal_manager.sql)
- 运行canal-admin，默认用户admin密码123456
- 在canal-admin中创建集群，并初始化主配置
```
# 若主配置未创建，则会导致如下异常
com.alibaba.otter.canal.common.CanalException: load manager config failed.
Caused by: com.alibaba.otter.canal.common.CanalException: requestGet for canal config error: canal.properties config is empty
```
- 参考canal_local.properties配置canal.properties，运行canal-server
```
canal.admin.passwd = select password('fs-project')
```
- 创建Instance实例配置，连接MySQL并写入RabbitMQ，或仅连库由自定义客户端读写数据

## 运行机制
- canal.serverMode只能配置在canal.properties中，不支持在destinations中单独定义。
- canal.mq.dynamicTopic仅支持“库_表”和“别名:.*\\..*”定义，自定义格式只能改源码。
- canal的client客户端目前仅支持连接单个destination实例。
- canal-admin集群模式下，无需单独修改主配置的canal.destinations字段。

## 最佳实践
- 若无时延要求，建议canal直接写mq消息队列，单独开发客户端过于浪费资源。
- 将canal.mq.topic定义为destination的名称，由队列消费端组合“实例.库.表.事件”。

## 参考
- [Canal Admin Docker](https://github.com/alibaba/canal/wiki/Canal-Admin-Docker)
- [Canal Admin 搭建 Canal 集群以及体验](https://www.itgrocery.cn/posts/99c08147/)
