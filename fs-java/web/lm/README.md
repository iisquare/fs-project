# lm(Large Model,大模型)


## 功能模块

### 功能简介

- 请求代理：根据授权和分组负载均衡、限制请求并发、是否启用敏感词检测。
- 智能体：配置客户端、系统提示词、温度（生成多样性）、最大生成数量等参数。
- 模型对话：敏感词、对话历史（树）、人工审核标注、用户反馈。
- 知识库管理：通过Excel导入数据、支持父子分段、支持按相关度和时间衰减排序。

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
