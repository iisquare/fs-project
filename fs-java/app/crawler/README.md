# spider-crawler
分布式可视化爬虫平台运行节点，本程序及相关示例仅供内部交流学习使用，请勿用于其他用途。

## feature
- 通过ZooKeeper/Curator实现分布式节点选注和配置管理
- 通过Redis.ZSet/Incr(TTL)实现延迟队列和优先级并发限制
    - 通过Token ZSet（线程数、优先级），获取执行权限
    - 通过Group Incr(TTL)（并发数），检查Token所在分组是否满足并发限制
    - 通过Schedule ZSet（延时队列），获取具体任务
- 支持对Schedule作业分组，限制全局或每节点的并发数
- 支持动态IP代理，可全局共享或指定给单个节点
- 通过Intercept拦截器和Assist协助处理器随机停顿或校验验证码

## future
- 增加请求类型支持
- 增加数据解析器支持
- 保持会话状态
- 自定义请求顺序
