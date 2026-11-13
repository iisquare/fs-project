# 智能体编排外部调用 API

版本：1.0  
更新日期：2026-11-11  
适用模块：`web/agent`

本文档面向需要通过 HTTP 调用智能体编排流程的外部系统。当前对外调用只执行已发布版本，不读取设计器草稿。

## 1. 接入流程

外部系统调用前需要由智能体模块管理员完成以下配置：

1. 在智能体编排中完成流程设计、保存并发布。
2. 确认编排状态为“启用”。
3. 如果编排配置了授权角色，确保最终生效用户拥有其中至少一个角色。
4. 在 LM 运营监控中创建模型访问密钥，其 secret 与 agent 配置项 `fs.agent.token` 一致。
5. 在 agent「API Key」页面为每个外部系统创建独立密钥；允许调用的流程可以留空，留空表示全部流程。
6. 把 Agent API Key 交给外部系统，作为调用 agent 时的 `x-api-key`。

外部系统传入的 `x-api-key` 只由 agent 校验。agent 调用 LM 时使用统一服务密钥：

```text
外部系统 -> agent：x-api-key = Agent API Key
agent -> LM：Authorization: Bearer fs.agent.token
              x-auth-token = 可选用户会话
```

Agent API Key 不需要配置 LM 模型权限。模型权限由 `fs.agent.token` 决定；LM 用量统一记录在
agent 服务密钥下，外部系统归属通过 agent 日志和 `traceId` 查询。

建议每个外部系统或应用使用独立的 Agent API Key，不要跨系统共用。

## 2. 基础信息

### 2.1 Base URL

```text
https://<agent-host>[:port]
```

如果部署在前置网关，请使用网关为 agent 服务分配的公开地址和路径前缀。本文示例假设 agent 服务可直接访问。

### 2.2 内容类型

普通调用：

```http
Content-Type: application/json
Accept: application/json
```

流式调用：

```http
Content-Type: application/json
Accept: text/event-stream
```

### 2.3 请求头

| Header | 必填 | 说明 |
| --- | --- | --- |
| `x-api-key` | 是 | Agent API Key；可配置流程范围，留空表示全部流程 |
| `Content-Type` | 是 | 固定为 `application/json` |
| `Accept` | 否 | 普通调用填 `application/json`；流式调用填 `text/event-stream` |
| `x-auth-token` | 否 | member 登录会话令牌，不带 `Bearer ` 前缀 |
| `x-external-user-id` | 否 | 外部系统终端用户标识，最多 128 个字符，仅用于审计 |
| `x-trace-id` | 否 | 调用链追踪标识，最多 128 个字符 |

`x-external-user-id` 不参与鉴权、积分和会话归属。没有 `x-auth-token` 时，会话仍归属 Agent API Key 所属用户。

`x-trace-id` 可包含字母、数字、`.`、`_`、`:`、`-`。未传或格式无效时，agent 会生成一个追踪标识，并通过响应返回。

### 2.4 用户代理

- 未传 `x-auth-token`：积分、会话、运行日志和授权角色均按 API Key 所属用户处理。
- 传入有效 `x-auth-token`：使用会话用户作为最终生效用户。
- 已传 `x-auth-token` 但会话无效：请求被拒绝，不会回退到 API Key 所属用户。

## 3. 接口列表

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/agentic/invoke` | 调用已发布流程；`stream=false` 或省略时返回 JSON，`stream=true` 时返回 SSE |

## 4. 非流式调用

### 4.1 请求

```http
POST /agentic/invoke
Content-Type: application/json
x-api-key: <Agent API Key>
x-auth-token: <用户会话，可选>
x-external-user-id: user-10001
x-trace-id: order-callback-20261111-001
```

```json
{
  "id": 123,
  "inputs": {
    "query": "帮我总结这段内容",
    "source": "external-system"
  }
}
```

### 4.2 请求参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | integer | 是 | 编排应用 ID |
| `inputs` | object | 视流程配置 | 开始节点声明的输入参数。推荐使用 JSON 对象 |
| `chatId` | integer | 否 | 多轮会话 ID。不传、传 `0` 或无效 ID 时创建新会话 |
| `parentId` | integer | 否 | 指定本轮消息挂载位置；显式传 `0` 表示从会话起点新建分支 |
| `reuseQuestion` | boolean | 否 | 是否重新生成。`true` 时不重复写入用户提问 |
| `stream` | boolean | 否 | `false` 或省略返回普通 JSON；`true` 返回 SSE |

`inputs` 的字段由流程开始节点定义。例如开始节点声明了 `query`、`userId`、`files`，调用方就按对应字段名传入。

开始节点中标记为必填的参数只在新建会话时校验。续写已有会话时使用会话中已保存的参数。

`inputs` 兼容 JSON 字符串形式，但推荐直接传对象。

### 4.3 成功响应

HTTP 状态通常为 `200`，业务结果以响应体中的 `code` 和 `data.status` 为准。

```json
{
  "code": 0,
  "message": null,
  "data": {
    "id": 123,
    "name": "订单处理助手",
    "mode": "workflow",
    "version": 8,
    "publishedTime": 1791686400000,
    "source": "published",
    "status": 1,
    "error": "",
    "answer": "已为你完成内容总结。",
    "outputs": {
      "start_1": {
        "query": "帮我总结这段内容"
      },
      "llm_1": {
        "text": "已为你完成内容总结。"
      }
    },
    "conversation": {},
    "steps": [
      {
        "id": "start_1",
        "type": "Start",
        "name": "开始",
        "status": 1,
        "duration": 1
      },
      {
        "id": "llm_1",
        "type": "LLM",
        "name": "总结",
        "status": 1,
        "duration": 2530
      }
    ],
    "duration": 2680,
    "traceId": "order-callback-20261111-001",
    "inputs": {
      "query": "帮我总结这段内容",
      "source": "external-system"
    },
    "chatId": 9001,
    "questionId": 30001,
    "answerId": 30002,
    "leafId": 30002,
    "logId": 70001
  }
}
```

### 4.4 响应字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | integer | API 层结果码，`0` 表示请求处理成功 |
| `message` | string/null | API 层提示信息 |
| `data.status` | integer | 流程执行状态：`1` 成功，`2` 失败 |
| `data.error` | string | 流程节点失败原因；成功时为空 |
| `data.answer` | string | 结束节点产生的最终回复 |
| `data.outputs` | object | 以节点 ID 为键的节点输出 |
| `data.conversation` | object | 本轮运行后的会话变量 |
| `data.steps` | array | 节点执行明细 |
| `data.duration` | integer | 本轮执行耗时，毫秒 |
| `data.id` | integer | 编排应用 ID |
| `data.name` | string | 编排应用名称 |
| `data.mode` | string | `workflow` 或 `chat` |
| `data.version` | integer | 实际执行的发布版本号 |
| `data.publishedTime` | integer | 发布时间，Unix 毫秒 |
| `data.source` | string | 外部调用固定为 `published` |
| `data.traceId` | string | 调用链追踪标识 |
| `data.inputs` | object | 本轮实际入参 |
| `data.chatId` | integer | 会话 ID，续写时回传 |
| `data.questionId` | integer | 本轮用户消息 ID；重新生成时为 `0` |
| `data.answerId` | integer | 本轮助手回复 ID |
| `data.leafId` | integer | 本轮完成后的会话分支尾消息 ID |
| `data.logId` | integer | agent 运行日志 ID；日志写入失败时为 `0` |

重要：外层 `code=0` 只表示接口完成处理，不代表流程一定成功。调用方必须继续判断 `data.status`：

- `data.status=1`：流程成功。
- `data.status=2`：流程内部节点失败，查看 `data.error` 和 `data.steps`。

## 5. 流式调用

### 5.1 请求

```http
POST /agentic/invoke
Content-Type: application/json
Accept: text/event-stream
x-api-key: <Agent API Key>
x-auth-token: <用户会话，可选>
x-external-user-id: user-10001
x-trace-id: order-callback-20261111-002
```

```json
{
  "id": 123,
  "stream": true,
  "inputs": {
    "query": "请分析这份数据"
  }
}
```

请求参数与非流式调用相同，并显式传入 `stream: true`。

### 5.2 SSE 格式

响应类型为：

```text
Content-Type: text/event-stream
Cache-Control: no-cache
```

每条消息使用标准 SSE `data:` 字段，内容是一个 JSON 对象：

```text
data: {"type":"delta","data":{"nodeId":"llm_1","content":"正在","reasoning":""}}
```

当前事件类型：

| `type` | 说明 |
| --- | --- |
| `delta` | 模型正文或思考内容增量 |
| `step` | 节点执行进度 |
| `round` | ReAct 轮次和工具调用进度 |
| `done` | 流程正常完成，`data` 与非流式成功响应结构一致 |
| `error` | 流式通道发生业务或系统错误 |

事件结构示例：

```text
data: {"type":"delta","data":{"nodeId":"llm_1","content":"正在分析","reasoning":""}}
data: {"type":"delta","data":{"nodeId":"llm_1","content":"","reasoning":"先检查数据字段"}}
data: {"type":"step","data":{"id":"llm_1","type":"LLM","name":"数据分析","state":"running"}}
data: {"type":"round","data":{"nodeId":"llm_1","round":1,"maxRounds":6,"state":"calling","call":{"id":"call_1","method":"queryData"}}}
data: {"type":"step","data":{"id":"llm_1","type":"LLM","name":"数据分析","state":"success","status":1,"duration":1520}}
data: {"type":"done","data":{"code":0,"message":null,"data":{"status":1,"answer":"分析完成","traceId":"order-callback-20261111-002","chatId":9002}}}
```

### 5.3 `delta` 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | string | 产生内容的节点 ID |
| `content` | string | 正文增量 |
| `reasoning` | string | 思考内容增量 |
| `replace` | boolean | 可选。为 `true` 时用 `content` 替换已累积正文 |
| `reasoningReset` | boolean | 可选。为 `true` 时清空已累积的思考内容 |

### 5.4 `step` 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 节点 ID |
| `type` | string | 节点类型 |
| `name` | string | 节点名称 |
| `state` | string | `running`、`success` 或 `failed` |
| `status` | integer | 完成事件中为 `1` 成功、`2` 失败 |
| `duration` | integer | 节点耗时，毫秒 |
| `container` | string | 可选。容器节点标识 |
| `iteration` | integer | 可选。容器内迭代序号，从 `0` 开始 |
| `error` | string | 可选。节点失败原因 |

### 5.5 `round` 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nodeId` | string | 产生轮次事件的节点 ID |
| `round` | integer | 当前 ReAct 轮次 |
| `maxRounds` | integer | 最大轮次 |
| `state` | string | `running`、`calling` 或 `completed` |
| `call` | object | 可选。工具调用信息，包括 `id`、`method`、`status`、`duration` 等 |

### 5.6 `error` 字段

```json
{
  "type": "error",
  "data": {
    "code": 9500,
    "message": "运行异常：模型调用失败"
  }
}
```

### 5.7 SSE 结束

agent 不发送额外的 `[DONE]` 哨兵。客户端收到 `done` 或 `error` 后应结束本轮接收，并同时处理连接关闭。

客户端实现必须：

- 按空行切分 SSE 消息。
- 忽略无法解析或未知字段，避免新增字段导致兼容性问题。
- 收到 `done` 后读取完整业务结果。
- 收到 `error` 后记录错误码和消息。
- 网络中断时保留已接收内容；服务端仍会继续完成本轮并写入日志。

## 6. 多轮对话

第一轮成功后保存响应中的 `data.chatId`：

```json
{
  "id": 123,
  "chatId": 9001,
  "inputs": {
    "query": "继续说明第二点"
  }
}
```

续写限制：

- `chatId` 必须属于同一编排应用、发布版本类型和最终生效用户。
- 不能跨 Agent API Key 所属用户或会员用户续写。
- 没有 `x-auth-token` 时，同一外部系统的不同终端用户仍属于 Agent API Key 所属用户。调用方应自行按外部用户保存对应的 `chatId`。

分支参数：

| 场景 | 参数 |
| --- | --- |
| 接在当前分支尾 | 不传 `parentId` |
| 从会话起点新建分支 | `parentId: 0` |
| 在指定消息后继续 | `parentId: <消息 ID>` |
| 重新生成指定用户消息的回复 | `parentId: <用户消息 ID>`，`reuseQuestion: true` |

## 7. 错误处理

### 7.1 HTTP 与业务码

多数业务错误仍返回 HTTP `200`，调用方必须解析响应体 `code`。未通过认证或接口权限拦截时可能返回 HTTP `403`。

| `code` | HTTP 状态 | 说明 |
| --- | --- | --- |
| `0` | 200 | 接口处理完成，继续检查 `data.status` |
| `403` | 403 | 未通过登录或接口权限校验 |
| `404` | 200 | 编排应用不存在 |
| `1001` | 200 | Agent API Key 无效、禁用、过期或 Secret 不匹配 |
| `1002` | 200 | 编排未发布、已停用或发布内容无效 |
| `1004` | 200 | 缺少开始节点必填参数 |
| `9403` | 200 | 流程不在 API Key 授权范围，或最终生效用户不满足编排授权角色 |
| `9500` | 200 | 执行过程中发生异常，`data.traceId` 可用于排查 |

### 7.2 错误示例

API Key 错误：

```json
{
  "code": 1001,
  "message": "无效的 Agent API Key",
  "data": null
}
```

缺少必填参数：

```json
{
  "code": 1004,
  "message": "缺少必填参数：用户问题",
  "data": "用户问题"
}
```

运行时异常：

```json
{
  "code": 9500,
  "message": "运行异常：模型调用失败",
  "data": {
    "traceId": "order-callback-20261111-003"
  }
}
```

## 8. 调用示例

### 8.1 cURL：非流式

```bash
curl -X POST 'https://agent.example.com/agentic/invoke' \
  -H 'Content-Type: application/json' \
  -H 'x-api-key: agt_xxxxxxxx.xxxxxxxxxxxxxxxx' \
  -H 'x-external-user-id: user-10001' \
  -H 'x-trace-id: req-20261111-001' \
  -d '{
    "id": 123,
    "inputs": {
      "query": "请处理这笔订单"
    }
  }'
```

### 8.2 cURL：流式

```bash
curl -N -X POST 'https://agent.example.com/agentic/invoke' \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -H 'x-api-key: agt_xxxxxxxx.xxxxxxxxxxxxxxxx' \
  -H 'x-trace-id: req-20261111-002' \
  -d '{
    "id": 123,
    "stream": true,
    "inputs": {
      "query": "请处理这笔订单"
    }
  }'
```

### 8.3 Python：非流式

```python
import requests

response = requests.post(
    "https://agent.example.com/agentic/invoke",
    headers={
        "Content-Type": "application/json",
        "x-api-key": "agt_xxxxxxxx.xxxxxxxxxxxxxxxx",
        "x-trace-id": "req-20261111-003",
        "x-external-user-id": "user-10001",
    },
    json={
        "id": 123,
        "inputs": {"query": "请处理这笔订单"},
    },
    timeout=600,
)
response.raise_for_status()

body = response.json()
if body.get("code") != 0:
    raise RuntimeError(body.get("message"))

data = body.get("data") or {}
if data.get("status") != 1:
    raise RuntimeError(data.get("error") or "智能体执行失败")

print(data.get("answer"))
print(data.get("traceId"))
```

## 9. 日志与追踪

agent 运行日志记录：

- 最终生效用户 `createdUid`
- Agent API Key ID
- Agent API Key 名称与属主（查询时按 ID 关联填充）
- 调用方式 `callerType`
- 是否使用用户代理 `userProxy`
- 外部终端用户 `externalUserId`
- 调用链标识 `traceId`

LM 用量统一记录到 `fs.agent.token` 对应的服务密钥，并使用相同的 `traceId`。
如果一次流程调用包含多个模型节点，可以用该标识关联 agent 总体运行记录与该轮全部 LM 调用。

调用链标识只用于追踪，不作为鉴权或幂等键。

## 10. 重试与幂等

当前接口不提供 `Idempotency-Key` 或自动去重。发生网络超时后直接重试，可能产生新的会话轮次、重复积分消耗和重复业务操作。

建议调用方：

- 业务侧生成唯一业务请求号，并自行实现幂等控制。
- 每次尝试保留同一个 `x-trace-id`，便于确认首次请求是否已进入服务端。
- 普通接口客户端超时建议不低于 600 秒。
- 流式接口使用支持长连接的 HTTP 客户端，不要使用过短的读取超时。
- 收到 `done` 后以 `answerId`、`chatId` 和 `logId` 作为本轮完成凭据。

## 11. 兼容性约定

- 响应对象可能增加新字段，客户端应忽略未知字段。
- SSE 事件可能增加新字段或新的事件类型，客户端应容忍未知类型。
- 文档中的 `outputs`、`conversation` 和 `steps` 内容取决于编排配置。
- `inputs` 参数由每个编排应用的开始节点定义，不由平台接口固定。
