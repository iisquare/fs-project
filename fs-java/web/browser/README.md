# 无头浏览器服务

该模块作为 Web 服务使用 Playwright Chromium 执行动态页面，并通过 ZooKeeper
`/runtime/browsers` 注册为独立节点类型。请求接口、浏览器执行池、会话管理和后续打码能力统一位于该模块。

## 启动

```bash
gradle :web:browser:installPlaywright
gradle :web:browser:build
```

服务提供以下接口：

- `POST /browser/fetch`：执行一次浏览器请求并返回渲染后的 HTML。
- `GET /node/state`：返回当前浏览器池负载。

页面请求示例：

```json
{
  "url": "https://example.com",
  "requestType": "GET",
  "contentType": "none",
  "charset": "UTF-8",
  "timeout": 30000,
  "waitUntil": "networkidle",
  "waitSelector": "#app",
  "payloadForm": {},
  "headers": {}
}
```

返回值与原 HTTP 采集结果结构一致，包含 `status`、`html`、`location`、
`coast`、`warning` 和 `exception`，因此采集端可以复用现有解析器与映射器。

采集端会根据节点上报的 `active`、`queued` 和 `maxConcurrency`
选择负载最低的浏览器节点。浏览器池没有空闲实例时立即返回 Busy，采集端将任务放回
Spider 队列，不会在采集端等待重试，也不降级为 HTTP 模式。

服务默认端口为 `7836`，配置位于 `src/main/resources/application.yml`。
后续的验证码识别、滑块处理、登录态持久化和会话复用应作为独立 service 接入浏览器执行池，
不要将 Playwright 对象暴露给 Controller 或采集线程。

该服务接收外部页面地址，生产环境应置于内网或网关访问控制之后，并在网关层限制来源和请求频率。
