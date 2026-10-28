# lm(Large Model,大模型)


## 功能模块

### 功能简介

- 请求代理：根据授权和分组负载均衡、限制请求并发、是否启用敏感词检测。
- 系统配置：供应商配置、模型配置、网关状态（请求代理的缓存与发布订阅）。
- 运营监控：授权密钥、速率限制、调用日志、用量统计、用户积分。
- 安全围栏：拦截关键词（敏感词），命中后按配置阻断或替换。

## 用户代理

授权密钥可开启「允许用户代理」：调用方在带密钥访问网关的同时，把最终用户的会话令牌放在请求头
`x-auth-token`（`member` 服务签发的会话标识，不带 `Bearer ` 前缀，与 `SessionIdResolver` 同一口径；
常量声明在 `web.core` 的 `FeignInterceptor.HEADER_AUTH_TOKEN`）上，网关按该用户记账。

- 只在密钥开启代理时读取该头；未开启代理、或调用方没带令牌（定时任务、服务端编排等）时沿用密钥所属用户；
- 带了令牌但会话已失效、或该用户积分不可用时直接返回 401 / 400，不会把用量静默记到密钥所属用户头上；
- 命中代理时，使用记录的 `uid` 是会话用户、`authId` 仍是密钥（`fs_lm_usage` 里 uid 与密钥所属用户不一致即为代理调用），
  余额判定、积分扣除与限流策略都取会话用户自己的积分配置（速率限制自然按会话用户分别计数）；
- 模型权限按两层判定：密钥的「模型限制」始终生效，模型上配置的「授权角色」按生效用户的角色算
  （未命中代理时是密钥属主，命中代理时是会话用户）；
- 该头既不会转发给供应商，也不会写入使用记录的请求头（`fs.lm.gateway.recordDetails` 开启时同样剔除），避免令牌泄露。

## 测试样例

### Chat Completions `/v1/chat/completions`

```json
{
  "model": "openai",
  "stream": false,
  "messages": [
    {
      "role": "system",
      "content": "你是一个智能助手，可以帮助用户查询天气信息。在回答天气相关问题时，请优先调用 get_weather 工具获取实时数据。"
    },
    {
      "role": "user",
      "content": "北京今天的天气怎么样？"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "查询指定城市的实时天气信息",
        "parameters": {
          "type": "object",
          "properties": {
            "city": {
              "type": "string",
              "description": "城市名称，如 北京、上海"
            },
            "unit": {
              "type": "string",
              "enum": ["celsius", "fahrenheit"],
              "description": "温度单位"
            }
          },
          "required": ["city"]
        }
      }
    }
  ],
  "tool_choice": "auto"
}
```

### Messages `/v1/messages`

```json
{
  "model": "authropic",
  "system": "你是一个智能助手，可以帮助用户查询天气信息。在回答天气相关问题时，请优先调用 get_weather 工具获取实时数据。",
  "messages": [
    {
      "role": "user",
      "content": "北京今天的天气怎么样？"
    }
  ],
  "max_tokens": 1024,
  "stream": true,
  "tools": [
    {
      "name": "get_weather",
      "description": "查询指定城市的实时天气信息。返回温度、天气状况、湿度等。",
      "input_schema": {
        "type": "object",
        "properties": {
          "city": {
            "type": "string",
            "description": "城市名称，如 北京、上海"
          },
          "unit": {
            "type": "string",
            "enum": ["celsius", "fahrenheit"],
            "description": "温度单位，默认为 celsius"
          }
        },
        "required": ["city"]
      }
    }
  ],
  "tool_choice": {
    "type": "auto"
  }
}
```

### Responses `/v1/responses`

- Responses（工具调用）
```json
{
  "model": "deepseek-v4-flash",
  "stream": true,
  "instructions": "你是一个智能助手，可以帮助用户查询天气信息。在回答天气相关问题时，请优先调用 get_weather 工具获取实时数据。",
  "input": "北京今天的天气怎么样？",
  "tools": [
    {
      "type": "function",
      "name": "get_weather",
      "description": "查询指定城市的实时天气信息",
      "parameters": {
        "type": "object",
        "properties": {
          "city": {
            "type": "string",
            "description": "城市名称，如 北京、上海"
          },
          "unit": {
            "type": "string",
            "enum": ["celsius", "fahrenheit"],
            "description": "温度单位"
          }
        },
        "required": ["city"]
      }
    }
  ],
  "tool_choice": "auto"
}
```

- Responses（多轮对话）
```json
{
  "model": "deepseek-v4-flash",
  "stream": false,
  "instructions": "你是一个乐于助人的助手。",
  "input": [
    {
      "type": "message",
      "role": "user",
      "content": "你好，我叫小明。"
    },
    {
      "type": "message",
      "role": "assistant",
      "content": "你好小明！有什么可以帮助你的吗？"
    },
    {
      "type": "message",
      "role": "user",
      "content": "我叫什么名字？"
    }
  ]
}
```

- Responses（非流式 + 思维链）
```json
{
  "model": "deepseek-v4-flash",
  "stream": false,
  "instructions": "你是一个数学家，请逐步推理。",
  "input": "求解方程 x² - 5x + 6 = 0",
  "reasoning": {
    "effort": "high"
  }
}
```

### 网页搜索 `/v1/tools/search`

透传 DeepSeek Search API，按 query 返回结构化搜索结果。请求体与官方接口一致，另需带上网关的
`model`（类型为「网页搜索」的模型），用于鉴权与计费；该字段在转发给供应商前会被移除。

```json
{
  "model": "web-search",
  "query": "DeepSeek V4 模型 发布",
  "count": 5,
  "need_content": true
}
```

网关不校验、不改写请求体，`query`、`count`、`ori_question`、`localization`、`need_content`
等字段的约束由供应商按官方文档执行；响应体（含错误报文）与状态码同样原样返回。

### 网页抓取 `/v1/tools/fetch`

透传 DeepSeek Fetch API，抓取单个网页并以 Markdown 正文返回。按官方接口要求，`url` 必填、
必须是含主机名的 HTTP/HTTPS 地址，且不能携带用户名或密码。

```json
{
  "model": "web-fetch",
  "url": "https://example.com/article"
}
```

网关不校验、不改写请求体；响应为 `{ "title": "...", "content": "..." }`。目标站点不可用、
被访问策略拦截或正文无法提取时，供应商的错误状态与错误报文会原样回传。

### 搜索与抓取的服务配置

- 供应商：在 lm 的供应商配置中添加 DeepSeek，调用地址填 `https://api.deepseek.com/v1`，
  认证标识填 API Key；
- 模型：分别创建类型为「网页搜索」「网页抓取」的模型，请求中的 `model` 使用模型别名
  （未配置别名时使用模型名称），模型所属供应商即转发目标；
- 计费方案：搜索、抓取统一选择「请求频次」方案，`qps` 为每秒请求数，
  `credits` 为单次请求消耗的积分；
- 请求频次：按供应商和工具类型分别计数，超过 `qps` 时网关直接返回 429，不再转发；
- 授权密钥：在密钥的模型限制中勾选对应的搜索/抓取模型，鉴权与角色授权与对话模型一致；
- 不需要额外的工具配置文件。

两类接口都按请求次数计费：供应商返回 HTTP 200 时扣除一次 `credits`；
4xx/5xx 等失败请求原样回传状态码与报文，不扣积分，但仍会写入调用日志。
