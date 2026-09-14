# lm(Large Model,大模型)


## 功能模块

### 功能简介

- 请求代理：根据授权和分组负载均衡、限制请求并发、是否启用敏感词检测。
- 系统配置：供应商配置、模型配置、网关状态（请求代理的缓存与发布订阅）。
- 运营监控：授权密钥、速率限制、调用日志、用量统计、用户积分。
- 安全围栏：拦截关键词（敏感词），命中后按配置阻断或替换。

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
