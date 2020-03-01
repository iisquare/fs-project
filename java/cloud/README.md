# cloud
微服务示例，非k8s部署方案

## Component
- Consul 注册中心
- Hystrix [hɪst'rɪks] 断路器
- Zuul 智能路由（性能较低）
- Ribbon [ˈrɪbən] （带状物） 客户端负载均衡 - 默认依赖
- Feign [feɪn] （伪装） 声明式客户端
- Turbine [ˈtɜ:baɪn] （涡轮机） 集群监控
- Sleuth [slu:θ] （足迹 侦探） 收集调用依赖 - 暂时移除
- Zipkin 链路追踪 - 暂时移除

## How to Run
- 在配置中心中创建config/springcloud-server-config,dev/data
- 启动:cloud:config:ConfigApplication.class
- 初始化配置/swagger-ui.html#/index-controller/syncretizeActionUsingGET

## State
- refresh:{POST /actuator/bus-refresh/{destination}}
- registry:{POST /actuator/service-registry}
    ```
    -d '{"status":"UP"}' # UP,DOWN,OUT_OF_SERVICE,UNKNOWN
    ```
- status:{POST /actuator/restart|shutdown}
- deregister:{PUT consul:8500/v1/agent/service/deregister/:service_id}

## Reference
- [Spring Cloud Consul](https://cloud.spring.io/spring-cloud-consul/single/spring-cloud-consul.html)
- [Consul - Agent HTTP API](https://www.consul.io/api/agent/service.html)
- [Spring Boot 2.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.0-Migration-Guide)
