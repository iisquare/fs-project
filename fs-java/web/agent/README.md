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

### 智能体编排（agentic）

- 应用管理：维护编排应用（名称、应用类型、标签、排序、状态、描述），并进入编排画布。
- 新增与修改都在编排画布内完成，列表页只做跳转、查看、发布与删除。
- 内容分两条线：**保存**得到草稿（`content`），只用于设计器调试运行；**发布**把当时的草稿固化为
  发布内容（`published_content`）并递增发布版本，外部调用只读取发布内容，草稿改动不影响线上。
- 未发布的编排调用 `invoke` 会被拒绝。

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST `/agentic/list` | 编排列表（不返回画布内容） | `agent:agentic:` |
| POST `/agentic/info` | 编排详情（含草稿内容与发布状态） | `agent:agentic:` |
| POST `/agentic/save` | 保存草稿（设计器新增与修改） | `agent:agentic:add` / `agent:agentic:modify` |
| POST `/agentic/publish` | 发布：草稿固化为发布内容，版本号递增 | `agent:agentic:add` / `agent:agentic:modify` |
| POST `/agentic/delete` | 删除编排（含发布内容） | `agent:agentic:delete` |
| POST `/agentic/config` | 状态下拉、排序数据（应用类型由前端字典定义） | `agent:agentic:` |
| POST `/agentic/run` | 调试运行：使用草稿内容 | `agent:agentic:` |
| POST `/agentic/invoke` | 外部调用入口：只读取已发布内容 | `agent:agentic:` |

`run` 在编排执行引擎接入前，先返回按连线推导的运行计划（执行顺序、节点清单与画布告警），
便于调试画布；`invoke` 返回发布内容与版本信息，供执行引擎或外部系统使用。

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
| POST `/knowledgeImage/url` | 知识库原图地址签发 | 无需后台权限，按知识库授权角色判定 |
| POST `/knowledgeImage/upload` | 编辑时上传图片，返回文件标识与展示地址 | 无需后台权限，按知识库授权角色判定 |

#### 知识库图片

文档中的图谱、图表等图片上传至文件服务，归档标识记录在 `fs_agent_knowledge_image`。
agent 只负责权限判定与地址签发，原图由文件服务的 `/raw/` 接口输出：

入库流程：

- 文档上传解析时按原始位置提取图片：PDF 按图片在页面上的纵坐标插入到对应段落之间
  （正文文本仍由 PDFTextStripper 生成，质量不受影响），docx 紧随所在段落、
  pptx 紧随所在幻灯片、xlsx 紧随所在工作表；页眉页脚、文本框、版式母版、图表等
  无法定位的图片追加在文末，避免丢失。
  同一张图在文档中重复引用只入库一次。旧版 doc/xls/ppt 仅提取文本。
- PNG/JPEG/GIF/BMP/WebP 原样上传，其余矢量格式转 PNG；小于 32px 的装饰图忽略。
- 解析结果以 `{{kb-image:i}}` 标记图片插入位置，入库时替换为 `![说明](kb:文件标识)`，
  说明取自图片文件名，作为检索与无障碍文本。
- 检索块的向量计算使用去掉图片引用的纯文本（保留说明），正文与索引仍保留引用本身。

```
POST /knowledgeImage/url
{ "knowledgeId": 1, "ids": ["abc123"], "expire": 1800000 }

{
  "code": 0,
  "data": {
    "abc123": "http://127.0.0.1:7812/raw/abc123.png?time=...&expire=...&token=..."
  }
}
```

- 鉴权规则：按请求会话识别登录用户，用户需命中知识库 `role_ids` 授权角色，
  知识库未配置授权角色时仅要求登录。
- 只有归属该知识库且在用的图片才会出现在返回值中，其余情况（无权限、不存在、不属于该知识库）
  一律不返回，调用方对缺失的图片使用自身默认图兜底，避免探测图片是否存在。
- 前端默认图放在前端项目的公共目录（`public/images/no-permit.png`），不由后端输出。
- 地址由文件服务签发，有效期默认 `fs.agent.image.expire`，可通过 `expire` 覆盖，取值区间 1 分钟至 24 小时；
  地址带时效校验码，**不可持久化**，正文只存稳定标识 `![说明](kb:{图片ID})`，渲染前实时签发。
- 图片入库时须保持 `sharable=0`（文件服务默认值），否则可通过文件服务的共享图片地址绕过本接口的鉴权。
- 删除文档或知识库时，图片对象与图片记录随之一并清理：文档删除时按 `document_id` 收集，
  与其他文件合并为一次 `/file/delete` 调用；知识库删除时按 `knowledge_id` 兜底清理残留图片。
- 前端经管理端代理调用，登录态由 Spring Session 的会话 Cookie 携带，无需额外传参。

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
| `agent:agentic:` | AgenticController | 智能体：应用管理（编排列表）、应用编排（画布、调试运行、发布、外部调用） |
| `agent:chat:` | ChatController | 模型对话：模型调试、模型对话、模型对比 |
| `agent:knowledge:` | KnowledgeController、KnowledgeDocumentController、KnowledgeSegmentController、KnowledgeChunkController | 知识库：知识管理、文档管理、分段管理、知识召回 |
| `agent:tool:` | ToolController | 插件管理：工具管理、MCP 服务 |
| `agent:skill:` | SkillController、SkillVersionController | 插件管理：技能管理、版本管理 |

## 前端页面

前端通过 `/api/{app}{uri}` 访问后端，智能体相关页面使用 `app=agent`：

| 路径 | 说明 |
| --- | --- |
| `/agent/agentic/list` | 智能体应用管理 |
| `/agent/agentic/model` | 智能体应用编排（画布：新增、修改、调试运行、发布） |
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
| `fs.agent.image.expire` | 原图地址签发有效期（毫秒），默认 1800000 |
| `rpc.file.rest` | 文件服务地址，图片上传与地址签发经此调用 |
| `fs.format.date` | 日期格式 |

服务端口默认 `7827`，服务名 `fs-agent-service`，
JPA 表前缀策略为 `com.iisquare.fs.web.agent.dsconfig.NamingStrategy`。

## 数据与存储

- MySQL：`fs_agent_agent`、`fs_agent_knowledge`、`fs_agent_knowledge_chunk`、
  `fs_agent_agentic`（编排草稿与发布内容）、`fs_agent_knowledge_document`、`fs_agent_knowledge_image`、
  `fs_agent_knowledge_segment`、`fs_agent_skill`、`fs_agent_skill_version`、`fs_agent_tool`，
  建表语句见 `docs/fs_project_agent.sql`。
- Elasticsearch：检索块集合 `fs_lm_knowledge_chunk`。
- 文件服务：桶 `fs-lm-knowledge`、`fs-lm-skill`。
- `Chat`、`ChatCompare`、`ChatDemo`、`ChatDialog` 为历史实体，不建表。
