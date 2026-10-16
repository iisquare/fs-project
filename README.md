# fs-project，Full Stack Project

技术中台、数据中台、AI 中台、业务中台一体化服务平台，通过组合各模块合成最终系统。

## 项目简介

项目以「中台组件 + 低代码」的方式承载企业应用的开发与交付，可拆分为三条相对独立、又相互复用的能力线：

- **低代码开发平台**：集成基础服务组件，含表单设计、工作流程设计、智能报表、大屏设计、应用设计、脚手架等功能；可有效节省项目开发成本，提高代码产出效率，保障项目开发质量，积累优质解决方案。在线设计完成的应用可直接发布使用，也可生成代码进行二次开发。
- **大数据分析和治理平台**：包含数据交换、定时任务、元数据管理、数据标准、数据质量、数据安全、数据资产、生命周期管理等模块。
- **大模型和智能体编排**：包含知识库、工具、技能、知识图谱、模型网关、安全围栏等模块。

## 项目结构

| 目录 | 说明 | 技术栈 |
| --- | --- | --- |
| `fs-java` | 后端服务，Gradle 多模块组织：`base` 基础库、`app` 计算应用、`ext` 计算扩展、`web` 业务服务 | JDK 17、Spring Boot 3.5、OpenFeign、JPA / MyBatis、Druid |
| `fs-admin` | 新版前端，管理后台与各类可视化设计器（自 `static/admin` 逐步迁移） | Vue 3 + TypeScript、Vite、Element Plus、Pinia、AntV X6 3.x |
| `fs-bot` | 智能体桌面客户端 | Electron 39、Vue 3 + TypeScript、better-sqlite3（本地加密库） |
| `fs-python` | 模型侧工程：`fs-gpt` 负责大模型训练、量化与推理，`fs-core` 预留 | Python 3.10、PyTorch、vLLM / SGLang |
| `python` | 早期模型实验代码：CRNN、EAST、InsightFace、LaMa、Mask R-CNN、MTCNN、多标签分类、NLP Transformer | Python |
| `static` | 旧版前端：`static/admin` 管理后台、`static/site` 多端站点、`static/resources` 静态资源 | Vue 2.6 + ant-design-vue 1.7、vue-cli、webpack |
| `docs` | 设计说明文档 | Markdown |

运行环境由独立项目维护：[fs-docker](https://github.com/iisquare/fs-docker) 提供单机测试环境，[fs-kubernetes](https://github.com/iisquare/fs-kubernetes) 提供高可用环境。

## 技术选型

- 后端基于 Spring Boot 3.5 以云原生方式开发，可快速转换为 Spring Cloud 或 K8S 等微服务运行模式。
- 管理后台基于 Vue 3 + Element Plus 开发；门户站点（`static/site`）沿用 webpack 多端方案，后续按收录需求考虑引入 Nuxt。
- 神经网络模型采用 Python 编写和训练，推理框架主要为 PyTorch，固化模型后交给 Java 端预测使用。
- 分布式爬虫采用 Java 开发，选型考虑主要为生态完整、资源调度方便，缺点是占用内存过大。
- 大数据计算采用有向图方式开发，支持 Spark、Flink 多种计算框架，可根据实际业务需求自由切换。
- 即时通讯规划采用 Java + Spring Boot + Netty，基于 Protobuf 制定多端通讯协议，支持大规模数据高并发读写；当前代码库仅有爬虫模块引入 Netty，尚无独立的 IM 服务。

### 版本基线

| 组件 | 版本 |
| --- | --- |
| JDK / Gradle | 17 / 7.6.5 |
| Spring Boot / Spring Cloud | 3.5.15 / 4.3.3 |
| MySQL | 8.0+，Druid 连接池 + JPA / MyBatis |
| Redis | Jedis 客户端，Spring Session 分布式会话 |
| Spark / Scala / Hadoop | 3.5.9 / 2.12 / 3.5.0 |
| Flink / Flink CDC | 2.2.1 / 3.6.0-2.2 |
| Node | 20.x（`fs-admin`）、22.x（`fs-bot`） |
| Python | 3.10（`fs-python`） |

## 功能模块

后端服务按 `fs-java/web` 分模块部署，新版前端 `fs-admin` 按模块组织路由与页面。下表为本地开发默认端口。

| 模块 | 后端模块 | 前端模块 | 端口 | 说明 |
| --- | --- | --- | --- | --- |
| 管理后台 | `web:admin` | 全局框架 | 7808 | 后台入口、`/api` 服务网关、文件与索引页 |
| 用户中心 | `web:member` | `views/member` | 7801 | 用户、角色、资源、菜单、字典、消息、应用 |
| 在线办公 | `web:oa` | `views/oa` | 7816 | 表单设计、流程设计、打印、在线审批 |
| 商业智能 | `web:bi` | `views/bi` | 7815 | 数据集成、数据加工、OLAP、报表、大屏 |
| 数据采集 | `web:spider`、`web:crawler` | `views/spider` | 7806 / 7826 | 采集调度、配置项与采集端 |
| 系统服务 | `web:cron`、`web:worker`、`web:file`、`web:lucene` | `views/server` | 7819 / 7813 / 7812 / 7810 | 定时任务、消息队列、文件存储、搜索引擎 |
| 数据治理 | `web:govern` | 占位路由 | 7820 | 元数据、血缘关系、数据标准、数据质量 |
| 知识图谱 | `web:kg` | `views/kg` | 7825 | 本体建模、知识抽取、知识融合、知识评估、知识检索 |
| 大模型网关 | `web:lm` | `views/lm` | 7824 | 供应商、模型、授权密钥、速率限制、安全围栏 |
| 智能体平台 | `web:agent` | `views/agent` | 7827 | 应用、对话、知识库、插件（工具、技能、MCP） |
| 内容管理 | `web:cms` | 占位路由 | 7817 | 内容维护 |
| 人脸识别 | `web:face` | 占位路由 | 7809 | 人脸检测、人脸识别、检索对比 |
| 门户与商城 | `web:site`、`web:shop` | — | 8080 / 7818 | 门户站点与商城 |
| 计算引擎入口 | `web:spark`、`web:flink` | — | 7822 / 7823 | Spark / Flink 作业提交与监控 |
| 演示实例 | `web:demo` | `views/demo` | 8033 | 基础组件、功能演示、示例代码 |

数据治理、内容管理、人脸识别在新版前端仅有 `/govern`、`/cms`、`/face` 的工作面板入口，完整功能仍保留在旧版前端 `static/admin` 中。

### 模块能力明细

- 用户中心：帐号、角色、资源、菜单、配置。
- 网页爬虫：通用采集、定向采集、任务分发、自动打码。
- 商业智能：数据集成、数据加工、数据服务、数据可视化。
- 人脸识别：人脸检测、人脸识别、检索对比。
- 搜索引擎：词典管理、索引示例、服务重载。
- 消息队列：工作节点、资源管理、队列分配。
- 定时任务：任务编排、业务RPC集成、作业管理、流程调度。
- 在线办公：表单设计、流程设计、在线审批。
- 演示实例：基础组件、功能演示、示例代码。
- 文件存储：文件存储、图库图床、对象存储。
- 项目管理：脚手架、页面设计、应用设计。
- 数据治理：元数据、血缘关系、数据标准、数据质量。
- 知识图谱：本体建模、知识抽取、知识融合、知识评估、知识检索。
- 大模型网关：安全围栏、供应商管理、积分授权、速率限制。
- 智能体平台：对比验证、知识库、工具插件、技能插件、智能体编排。

## 快速开始

### 后端 fs-java

环境：JDK 17、Gradle 7.6.5、MySQL 8.0+、Redis；可选 MongoDB、Elasticsearch、Neo4j、Kafka、RabbitMQ。

```bash
cd fs-java
gradle :web:admin:bootJar                    # 构建管理后台
gradle :web:member:bootJar                   # 构建用户中心
java -jar web/admin/build/libs/fs-project-web-admin.jar --spring.profiles.active=dev,idev
```

### 前端 fs-admin

环境：Node 20、pnpm 10。

```bash
cd fs-admin
pnpm install
pnpm run dev      # 开发服务，已默认开启 --host
pnpm run build    # 类型检查 + 构建
```

构建产物输出到 `fs-java/web/admin/src/main/resources/static`，并自动把 `index.html` 拷贝至模板目录，便于随后端一并发布。

### 桌面端 fs-bot

环境：Node 22、pnpm 11；`better-sqlite3-multiple-ciphers` 依赖 Python 环境，首次安装建议在 Anaconda Prompt 中执行。

```bash
cd fs-bot
pnpm install
pnpm dev          # 开发模式
pnpm build:win    # 打包 Windows 安装包，需管理员权限
```

### 模型侧 fs-python

`fs-gpt` 使用 conda 环境（Python 3.10），支持全参数微调、LoRA / QLoRA、GPTQ / AWQ 量化与 vLLM / SGLang 推理，详见 [fs-gpt 说明](fs-python/fs-gpt/README.md)。

## 效果示例

| 功能 | 示例 | 描述 |
| :----- | :----- | :----- |
| 规则设计器 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-dag-diagram.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-dag-diagram.mp4) | 流批一体规则编排 |
| 表单设计器 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/oa-form-design.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/oa-form-design.mp4) | 无 |
| 页面设计器 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/auto-layout-design.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/auto-layout-design.mp4) | 无 |
| 流程设计器 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/oa-flow-design.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/oa-flow-design.mp4) | 无 |
| 数据钻取报表 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-drill-table.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-drill-table.mp4) | 支持多维度多度量钻取 |
| 数据分布报表 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-distribution-table.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/bi-distribution-table.mp4) | 支持任意维度分组统计 |
| 元数据血缘分析 | [gif](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/govern-meta-blood.gif)/[mp4](https://raw.githubusercontent.com/iisquare/fs-project-resource/main/static/demo/govern-meta-blood.mp4) | 支持元数据检索和关联分析 |
| 智能体编排 | [png](https://raw.githubusercontent.com/iisquare/fs-project-resource/refs/heads/main/static/demo/agent-flow.png) | 支付大模型和接口服务流程编排 |
| 本地智能体客户端 | [png](https://raw.githubusercontent.com/iisquare/fs-project-resource/refs/heads/main/static/demo/bot-chat.png) | 支持服务端工具集成和本地技能命令执行 |

## 核心能力

### 用户中心与权限

- 应用、资源、菜单、角色与授权关系集中维护，权限键按 `应用:控制器:动作` 组织（如 `agent:agentic:`、`bi:dataQueryLog:delete`），前后端按同一套资源校验。
- 组织架构由部门、岗位、分组构成，控制数据访问范围与流程指派；数据权限按角色配置过滤条件与可见字段，并保留变更记录。
- 数据托管：自定义表单的模型定义（对象、主键、字段）由平台维护，业务数据按模型自动托管，支持定制化筛选与高级搜索，也可手动关联已存在的物理表。
- 配套数据字典、站内消息、收藏与系统设置。

### 文件存储与搜索引擎

- 文件基于 S3 对象存储（当前部署 RustFS），支持极速秒传、分段上传、分段下载与客户端直传。
- 图片按分享、原图、下载三种路径访问：分享图可缩放、转码、加水印且支持匿名访问，原图与下载走时效校验码。
- 搜索引擎基于 Lucene 集中维护词典与索引示例，支持服务重载。

### 自定义表单

- 拖拽式表单设计器，同时保存设计内容与数据存储配置，配合正则校验库完成字段级校验。
- 默认自动托管数据，支持定制化筛选和高级搜索；系统不提供数据库设计器（推荐模型同步），可手动关联已存在的物理表。
- 支持子表单、数据字典、组织架构等自定义组件。
- 集成脚手架，可逆向生成项目代码。

### 自定义工作流

- 流程设计器基于 BPMN，工作流定义绑定表单，可定制化表单读写权限，并支持逆向生成项目代码。
- 审批闭环：新建单据、待签任务、待办任务、历史任务；一个流程模型可多次部署，流程实例可挂起、撤销、删除。
- 打印模板保存设计 JSON、渲染 HTML 与内容，随表单与流程输出。

### 数据分析和智能报表

- 数据集维护类型、查询表达式、字段、主键、分区与授权角色，支持多数据源聚合；数据主题把若干数据集组织成面向主题的查询范围，供报表与智能体复用。
- 数据矩阵通过矩阵配置项精确匹配数据统计项，可视化报表支持多维度多度量钻取与任意维度分组统计。
- 跨数据源即席查询覆盖 API 接口、Excel 文件、关系型数据库、文档型数据库与数据湖；查询明细统一记录查询类型、执行 SQL、行数、耗时、失败原因与来源 IP。

### 自定义大屏和应用设计器

- 提供 DataV 基础设计器面板，集成自定义报表组件和装饰效果。
- 组态可视化，支持 2D 和 3D 组件交互、钻取、联动、跳转。
- 个性化定制页面内容，所见即所得生成定制化应用。

### 流批一体计算编排

- 计算规则以有向无环图描述，按计算集群（spark / flink、local / remote）与处理模式（batch / stream）组合提交，同一份编排可在 Spark 与 Flink 之间自由切换。
- 支持 Cron 定时调度、消息通知与失败自动恢复；每次运行保留编排快照、运行参数、调试输出与预警信息。
- 数据处理链路参考 [docs/dw-etl.md](docs/dw-etl.md) 的分层设计，数据变更同步参考 [docs/data-sync.md](docs/data-sync.md)。

### 分布式任务调度

- 基于 Zookeeper 与 Quartz 开发，支持节点、作业与触发器管理。
- 流程编排、流程阶段与调度日志组成任务编排与日志管理，业务 RPC 调用单独留痕。
- 采用 Antv X6 开发任务工作流设计，支持手动触发与参数覆盖。

### 数据采集

- 服务端负责站点、模板与页面配置、任务分发，并统一管理限速、拦截与黑白名单。
- 采集端（`app:crawler`）按任务抓取并回传结果，文件类响应转存文件服务；通用采集、定向采集与自动打码由采集端与识别端协作完成。
- 清洗端按页面类型判定（首页 / 列表页 / 详情页）加工为业务数据，正文图片转存图床。

### 数据治理

- 元数据管理：采集数据源与元模型（含字段与关联），通过主外键建立物理连接、通过元模型应用建立逻辑连接，进而形成血缘关系。
- 数据标准、质量规则、质量方案与逻辑、质检日志构成数据质量管理闭环。
- 数据资产考评可页面查看，质检报告可对接自定义分析报表。

### 知识图谱

- 本体建模：定义实体、关系、属性与标签，本体定义保存在 MySQL，图数据存于 Neo4j。
- 知识抽取基于本体挂接关系数据库导入图库；知识融合通过规则、任务、候选与融合记录完成消歧去重。
- 知识评估与知识检索提供查询与留痕，本体可作为工具暴露给智能体编排。

### 大模型网关

- 供应商与模型配置集中管理调用地址、密钥与模型参数，兼容 OpenAI 与 Anthropic 协议，支持 SSE 流式转发与按会话路由。
- 请求代理按授权与分组负载均衡、限制请求并发；授权密钥可开启「用户代理」，网关按请求头 `x-auth-token` 携带的最终用户记账。
- 运营监控覆盖速率限制、调用日志与用量统计、用户积分。
- 安全围栏按关键词拦截，命中后按配置阻断或替换。

### 智能体编排

- 编排画布基于 AntV X6：列表页只做跳转、查看、发布与删除，新增与修改都在画布内完成；内容分两条线——**保存**得到草稿，仅供设计器调试运行；**发布**把当时的草稿固化为发布内容并递增版本，外部调用只读发布内容，未发布的编排会被拒绝调用。
- 流程节点：开始、结束、大语言模型、输出图表、知识检索、问题分类器、参数提取器、条件分支、迭代、循环、变量聚合器、变量赋值、代码执行、模板转换、文档提取器、列表操作、时间、HTTP 请求。执行引擎只负责画布解析、执行顺序推导、分支走向、步骤日志与错误中断，节点实现按类型拆分，新增节点类型只需添加一个实现类。
- 大语言模型节点可挂载工具并支持流式输出，调度策略三选一：`none`（不带工具的单次调用）、`functionCalling`（携带工具但只执行一轮）、`react`（工具结果回填上下文循环迭代，轮次上限可配置）。
- 工具五类：**工具方法**（从 OpenAPI / MCP 解析出的方法，参数可逐项绑定执行变量）、**知识库**（模型给出 query，节点召回后回填）、**编排应用**（把另一个编排当作工具调用）、**数据主题**（在主题范围内执行 SQL 查询并记录数据主题查询日志）、**知识图谱本体**（模型给出实体类型与关键词，走本体检索）。发布时校验工具与方法并把方法定义快照进发布内容，工具后续变动不影响已发布编排。
- 变量引用统一为 `{{#节点标识.变量名#}}`，整串引用保留原始类型（对象、数组），与固定文本混排时按文本替换；系统变量 `sys.appId`、`sys.userId`、`sys.userName`、`sys.conversationId`、`sys.datetime`、`sys.date` 由运行上下文注入。
- 多轮对话：消息按父子关系组成消息树，支持编辑提问与重新生成（新起分支、原分支完整保留），并可按消息回看当轮执行明细；记忆按节点逐个配置（范围 `conversation` / `user`、窗口轮数、是否把历史工具调用一并带回）。会话按「同一编排 + 同一类型 + 同一用户」续写，调试运行与发布应用的会话互不干扰。
- 可观测性：每次运行记录来源、发布版本、状态、耗时、入参、输出、逐节点步骤、失败原因与来源 IP；流式接口按 `delta`（模型增量）、`step`（节点进度）、`done`、`error` 推送事件，画布实时给节点与连线着色；流程统计给出会话数量与对话轮次时间轴、流程排名与用户排名。
- 授权角色控制编排应用可用范围，为空表示所有登录用户可用；交付入口包含调试运行、外部调用、流式调用与授权应用清单。

### 知识库与插件

- 知识库配置词嵌入与重排模型、召回方式与范围、topK 与分数阈值；文档、分段、检索块与内嵌图片分级维护，检索块同步到 Elasticsearch 供召回。
- 工具支持自定义 schema 与 MCP 服务两类：OpenAPI 文档（JSON / YAML，含 `$ref`、`allOf`）与 MCP 工具清单解析出方法后缓存，重解析会更新方法清单并标记消失的方法，人工排序与启停不被覆盖。
- 技能与技能版本以版本包形式上传文件服务，客户端按授权角色获取可用清单。
- 项目分享把客户端项目配置按版本快照发布到服务端，支持授权、启用、版本回看与导出。

### 桌面客户端

- `fs-bot` 基于 Electron + Vue 3，使用本地加密 SQLite 离线保存项目与会话：项目包含模型、上下文窗口、系统提示词、长期记忆、工具、环境变量与工作区，会话消息按父子关系组成消息树。
- 与服务端对接：拉取模型清单、技能清单与授权范围内的分享项目，并把本地项目按版本发布为分享项目。

### 其他模块

- 人脸识别：人脸分组、照片与用户关联，提供人脸检测、识别与检索对比。
- 内容管理：栏目、文章、标签、评论、反馈与引用。
- 门户与商城：门户站点由 `static/site` 提供多端（web / wap）静态页面，含行政区划等维度数据。

## 特性实验室
- 人脸检测和识别。
- 图像语义分割和分类。
- 图片和视频去水印。
- 图片隐藏盲水印。
- CSS字体加解密。
- 手势和滑块验证码。

## 开发计划
- 数据可视化，自定义报表、大屏。
- 打印模板设计器开发（分文档型、组件型，可套打、续打）。

## 开源协议

- 个人可无偿使用本项目下的所有源码，但需保留本项目的来源和标识。
- 商业或企业团体使用需取得本项目的授权，最终解释权归本项目所有。
- 获得授权仅代表可商业使用本项目下的全部源代码，但不包含售后和问答支持。
- 本项目提供有偿问答和定制化服务，可[联系作者](mailto:iisquare@163.com)商讨具体事宜。
- 本项目接收个人或商业赞助，赞助者的提案或反馈将优先被采纳和处理。
