# agent(智能体)

智能体应用、模型对话、知识库、插件管理（工具、技能、MCP）。

## 功能模块

### 智能体

- 应用管理：配置客户端、系统提示词、温度（生成多样性）、最大生成数量、自定义参数、授权角色。
- 应用按登录身份与角色过滤：仅返回当前用户角色可见且启用的应用。

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST `/agent/list` | 应用列表 | `agent:agent:` |
| POST `/agent/save` | 新增或修改应用 | `agent:agent:add` / `agent:agent:modify` |
| POST `/agent/delete` | 删除应用 | `agent:agent:delete` |
| POST `/agent/config` | 状态下拉数据 | `agent:agent:` |

### 模型对话

- 模型调试：临时指定模型、系统提示词、温度、最大生成数量与自定义参数。
- 模型对话：以智能体应用为入口，按应用参数拼装请求并流式返回。
- 模型对比：同一问题并行请求多个模型，便于对比输出。

对话请求经模型网关 `/v1/chat/completions` 转发（`rpc.lm.rest`），
鉴权、限流、敏感词检测、调用日志与积分由网关统一处理。

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST `/chat/demo` | 模型调试（SSE） | `agent:chat:demo` |
| POST `/chat/dialog` | 模型对话（SSE） | `agent:chat:dialog` |
| POST `/chat/compare` | 模型对比（SSE） | `agent:chat:compare` |

### 知识库

- 知识库：配置词嵌入模型、重排模型、召回数量与阈值、召回方式（向量、全文、混合）、
  召回范围（检索块、父子分段、全文）、分段与分块长度、重叠长度、标签与授权角色。
- 文档管理：上传文档（PDF、Word、Excel、PPT 等），解析为 Markdown，按分段、分块策略切分入库。
- 分段管理、分块管理：维护父子分段与检索块。
- 知识召回：按知识库配置做向量召回、全文召回或混合召回，可重排后返回。

词嵌入与重排通过模型网关的 `/v1/embeddings`、`/v1/rerank` 完成。

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST `/knowledge/list`、`/knowledge/info`、`/knowledge/config` | 知识库列表、详情、下拉数据 | `agent:knowledge:` |
| POST `/knowledge/save`、`/knowledge/delete` | 保存、删除知识库 | `agent:knowledge:add`、`agent:knowledge:modify`、`agent:knowledge:delete` |
| POST `/knowledge/embedding` | 重建/补充检索块向量 | `agent:knowledge:add`、`agent:knowledge:modify` |
| POST `/knowledge/recall` | 知识召回 | `agent:knowledge:` |
| POST `/knowledgeDocument/*` | 文档上传、列表、详情、保存、删除 | `agent:knowledge:*` |
| POST `/knowledgeSegment/*`、`/knowledgeChunk/*` | 分段、分块维护 | `agent:knowledge:*` |
| GET `/maintain/createChunk` | 创建检索块索引（运维接口） | 维护接口 |

### 插件管理

- 工具：类型支持 `schema`（自定义）与 `mcp`（MCP 服务），配置调用地址、请求头、查询参数与授权角色；
  `mcpSync` 可探测 MCP 服务提供的工具、资源与提示词清单。
- 技能与技能版本：维护技能基本信息与版本包，版本包上传至文件服务。
- MCP 服务：以 `spring-ai-mcp-server-webmvc` 暴露本地工具（`DemoTool`、`BiTool`），
  供模型通过模型上下文协议调用。

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST `/tool/list`、`/tool/info`、`/tool/config`、`/tool/mcpSync` | 工具列表、详情、下拉数据、MCP 探测 | `agent:tool:` |
| POST `/tool/save`、`/tool/delete` | 保存、删除工具 | `agent:tool:add`、`agent:tool:modify`、`agent:tool:delete` |
| POST `/skill/*`、`/skillVersion/*` | 技能与技能版本维护 | `agent:skill:*` |

## 权限

权限键为 `agent:{controller}:{action}`，资源归属智能体应用（`application_id = 308`）。

| 权限 | 覆盖的控制器 | 对应页面 |
| --- | --- | --- |
| `agent:agent:` | AgentController | 智能体：应用管理 |
| `agent:chat:` | ChatController | 模型对话：模型调试、模型对话、模型对比 |
| `agent:knowledge:` | KnowledgeController、KnowledgeDocumentController、KnowledgeSegmentController、KnowledgeChunkController | 知识库：知识管理、文档管理、分段管理、知识召回 |
| `agent:tool:` | ToolController | 插件管理：工具管理、MCP 服务 |
| `agent:skill:` | SkillController、SkillVersionController | 插件管理：技能管理、版本管理 |

## 前端页面

前端通过 `/api/{app}{uri}` 访问后端，智能体相关页面使用 `app=agent`：

| 路径 | 说明 |
| --- | --- |
| `/agent/agentic/list` | 智能体应用管理 |
| `/agent/chat/demo`、`/agent/chat/dialog`、`/agent/chat/compare` | 模型调试、模型对话、模型对比 |
| `/agent/knowledge/list`、`/agent/knowledge/document`、`/agent/knowledge/segment`、`/agent/knowledge/recall` | 知识库管理 |
| `/agent/plugin/tool`、`/agent/plugin/mcp`、`/agent/plugin/skill`、`/agent/plugin/skillVersion` | 插件管理 |

## 部署配置

| 配置项 | 说明 |
| --- | --- |
| `spring.datasource.agent.*` | 数据源：url、username、password、`table-prefix=fs_agent_` |
| `rpc.agent.name`、`rpc.agent.rest` | 服务注册信息，管理端 `/api/agent/**` 按 `rpc.agent.rest` 转发 |
| `rpc.lm.rest` | 模型网关地址，对话转发、词嵌入、重排经此调用 |
| `fs.lm.token` | 访问模型网关的令牌 |
| `fs.format.date` | 日期格式 |

服务端口默认 `7827`，服务名 `fs-agent-service`，
JPA 表前缀策略为 `com.iisquare.fs.web.agent.dsconfig.NamingStrategy`。

## 数据与存储

- MySQL：`fs_agent_agent`、`fs_agent_knowledge`、`fs_agent_knowledge_chunk`、
  `fs_agent_knowledge_document`、`fs_agent_knowledge_segment`、`fs_agent_skill`、
  `fs_agent_skill_version`、`fs_agent_tool`，建表语句见 `docs/fs_project_agent.sql`。
- Elasticsearch：检索块集合 `fs_lm_knowledge_chunk`。
- 文件服务：桶 `fs-lm-knowledge`、`fs-lm-skill`。
- `Chat`、`ChatCompare`、`ChatDemo`、`ChatDialog` 为历史实体，不建表。
