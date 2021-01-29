# worker

消息队列消费端

## 最佳实践
- 尽量采用RabbitAdmin或管理界面建立和绑定队列，避免使用注解。
- 通过动态监听消费消息队列，实现消费者数量动态控制，避免使用注解。

## 参考
- [RabbitMq动态添加监听](https://my.oschina.net/pentakill/blog/4748966)
