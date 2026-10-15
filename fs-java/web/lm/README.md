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
