# lm(Large Model,大模型)


## 功能模块

### 功能简介

- 请求代理：根据授权和分组负载均衡、限制请求并发、是否启用敏感词检测。
- 智能体：配置客户端、系统提示词、温度（生成多样性）、最大生成数量等参数。
- 模型对话：敏感词、对话历史（树）、人工审核标注、用户反馈。
- 知识库管理：通过Excel导入数据、支持父子分段、支持按相关度和时间衰减排序。

## 测试样例

### 调试数据

- OpenAI
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

- Authropic
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