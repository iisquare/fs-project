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

`run` 执行**草稿内容**并返回运行结果（回复内容、各节点输出、逐节点执行步骤），`invoke` 执行**发布内容**
并返回同样的结构，两者都会写入运行日志（`fs_agent_agentic_log`）。

### 编排调度（AgenticRunner）

- 代码结构：`AgenticRunner` 只做调度（画布解析、执行顺序、步骤日志、分支走向、错误中断、结果组装），
  节点实现按类型拆到 `com.iisquare.fs.web.agent.runner` 包下（`XxxNodeHandler`，实现 `AgenticNodeHandler`）；
  共用能力放在 `com.iisquare.fs.web.agent.core`：
  `AgenticNodeContext`（节点上下文：配置、入参、上游输出、会话变量、历史对话、变量解析、模型与工具调用）、
  `AgenticRuntime`（变量解析、模型网关、工具调用、多模态、记忆窗口等运行时能力）、
  `AgenticScheduler`（容器节点执行内部子图的调度接口，由 AgenticRunner 实现）。
  新增节点类型只需加一个 `@Service` 实现类，无需改动调度器；
- ReAct 工具（大语言模型可调用的能力）按类型拆分到 `com.iisquare.fs.web.agent.react` 包：
  `AgenticTool`（接口，含 function 定义的通用组装）、`AgenticToolService`（按 kind 注册与分派）、
  `MethodTool`（工具方法，外部调用）、`KnowledgeTool`（知识库）、`ThemeTool`（数据主题）、
  `AgenticInvokeTool`（编排应用）；新增工具类型只需加一个实现类；
- 按连线从开始节点推导执行顺序（条件分支按命中的 case 只走对应的边），逐节点执行并记录步骤明细；
- 变量引用统一为 `{{#节点标识.变量名#}}`：整串就是一个引用时返回原始值（保留对象/数组），
  否则按文本替换，因此「固定字符串 + 变量」可以混排（工具执行变量、模板、提示词都是同一套规则）；
  设计器里所有变量字段都按这套规范写入（下拉选择与手工输入的 `sys.xxx` / `conversation.xxx` 都写成占位符），
  「变量赋值」的目标变量同样按引用定位：`{{#容器标识.变量名#}}` 写回对应容器的作用域（嵌套容器里的同名变量也能区分）、
  `{{#conversation.变量名#}}` 写会话变量；运行时兼容历史数据里直接存的裸取值（容器作用域键、会话变量名）；
- 系统变量（`{{#sys.xxx#}}`，由运行上下文注入，一次运行内保持不变）：`sys.appId` 应用标识、
  `sys.userId` / `sys.userName` 调用人、`sys.conversationId` 会话标识、
  `sys.datetime` 当前时间（东八区，`yyyy-MM-dd HH:mm:ss`）、`sys.date` 当前日期（东八区，`yyyy-MM-dd`）；
  用户输入与用户文件属于开始节点自身，直接引用开始节点的 `query` / `files` 输出，不在系统变量里重复；
- 已支持节点：`Start`（入参）、`End`（回复内容与输出变量）、`Template`、`HTTP`、
  `LLM`（模型网关 `/v1/chat/completions`，地址取 `rpc.lm.rest`，认证密钥取 `fs.agent.token`，整个 agent 服务共用）、
  `Chart`（输出图表：把上游数据交给模型按内置提示词归纳成图表定义）、
  `VariableAggregator`（分组聚合）、`VariableAssigner`（会话变量赋值）、`SwitchCase`（条件分支）；
  其它节点类型会返回「节点类型暂未支持」并中断执行，日志里会标明失败节点。
- 大语言模型节点支持流式输出：`/agentic/runStream`（SSE）逐段推送模型增量，事件为
  `{ type: delta|step|done|error, data }`：`delta.data` 为 `{ nodeId, content, reasoning }`；
  `step.data` 为节点执行进度 `{ id, name, type, status, duration, state }`（`state` 取 running / success / failed，
  容器内节点还带 `container` 与 `iteration`），设计器据此把流程图上的节点与连线按「执行中蓝、成功绿、失败红」实时着色，
  跑完不再整体回放，只定位失败节点；子编排（编排工具）执行时同样不下发外层步骤事件；
  非流式入口（`/run`、`/invoke`、工具调用）仍走一次性返回；
- 大语言模型节点的工具清单支持五种能力（`tools[].kind`）：
  `method` 工具方法（含参数级执行变量绑定）、`knowledge` 知识库（模型给 query，节点召回后回填）、
  `agentic` 编排应用（模型给 query，作为开始节点入参调用另一个编排的发布内容，子编排不写会话与日志）、
  `theme` 数据主题（对应 /bi/data/theme 的主题：不带 sql 返回主题数据字典——数据集与字段、关联关系，
  带 sql 则按主题范围内已发布数据集执行查询并返回结果，查询会带上主题标识，供 BI 侧记录数据主题查询日志）；
  `ontology` 知识图谱本体（KG 模块，每个本体定义一个工具：本体内声明的实体类型作为参数枚举暴露给模型，
  模型给出实体类型与关键词后走 `/graph/search` 返回命中的实体数据）；
  知识库与编排工具的参数固定为 `query`，数据主题工具的参数为 `sql` / `limit`（均选填）；
  函数名与描述由编排侧配置，发布时会校验目标是否存在（编排需已发布）；
- 大语言模型节点的调度策略（`agentStrategy`）：
  `none` 不带工具、单次模型调用；`functionCalling` 携带工具、**只执行一轮**（模型给出的工具调用执行后即返回该轮输出）；
  `react` 循环迭代（工具结果回填上下文继续推理，直到模型给出非工具调用的回复），
  轮次上限取节点配置的 `maxIterations`，跑满上限仍在请求工具时按节点异常处理；
  每轮的模型输出与该轮工具调用记录在节点输出的 `rounds` 里，调试面板与运行日志按调用链展示；
- 最终回复里的图表：由「输出图表」节点（`Chart`）产生——节点不需要配置图表类型与图表要求，
  默认只取本轮的记录：用户问题（开始节点入参，去掉文件列表）与上游大语言模型节点的本轮输出
  （回复文本 + 工具调用明细，工具结果里就是真实数据）；流程里没有大语言模型节点时
  （如 HTTP 直接接图表），才退回使用其它上游节点的输出；
  不做长度截断，历史按节点自己的记忆配置取用（默认「仅用户提问」，见下节「记忆（多轮上下文）」），
  再按内置提示词要求模型只返回一个图表定义 `{"type","title","source","categories","series"}`，
  `type` 取 bar / line / pie / table，由模型按数据与问题自行选择（对比用柱状、趋势用折线、占比用饼图、明细用表格）；
  数据必须来自上下文并按问题口径归纳（分组汇总、排序取前 10 项等），不是把中间查询结果直接搬到回复里；
  上下文里没有可绘图的数据或问题与数据无关时模型返回 `{"type":"none"}`，节点不产出图表、回复照常；
  节点输出 `hasChart`（本轮是否真的产出了图表：模型判定无需绘图时为 `false`，可直接用于条件分支）、
  `type` / `title` / `source` / `categories` / `series` / `text`，可被下游节点引用；
  其中的 `placeholder`（图表占位符，形如 `[[chart:节点标识]]`，是正文里的一行标记而不是画布位置）
  用于在回复内容里指定图表位置：
  结束节点的回复内容写成「正文 + `{{#图表节点.placeholder#}}` + 正文」即可把图表渲染在中间，
  没有引用到的图表追加在回复末尾，模型判断无需绘图（`type=none`）时占位符会被自动去掉、不留痕迹；
  占位符会保留在回复文本里（前端按它分段渲染，复制时自动去掉），接口对接需要纯文本时不要引用它；
  运行结果按画布顺序收集这些图表到 `charts`，前端在回复气泡里渲染表格 / 柱状 / 折线 / 饼图并允许切换；
  图表右上角可下载 PNG 图片（导出时把标题写进画布，表格视图需先切到图表视图）；
  图表随会话消息持久化（消息的 `reference` 里带上 `charts`，`chatInfo` 按消息回带），
  历史记录打开后与当时展示一致，工具调用明细里仍只保留参数与返回原文；
- 运行日志记录：编排、来源（draft/published）、版本、状态、耗时、入参、输出（含回复内容）、
  逐节点步骤与失败原因、来源 IP；运行日志入口在「流程管理」列表页（按编排标识带 `agenticId` 打开日志页），
  「对话历史」里每条助手回复也可展开对应的完整执行过程。

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST `/agentic/run` | 调试运行（草稿内容），返回结果并写日志 | `agent:agentic:` |
| POST `/agentic/invoke` | 外部调用（发布内容），返回结果并写日志 | `agent:agentic:` |
| POST `/agentic/invokeStream` | 外部调用的流式版本（发布内容，SSE，供用户对话页展示流式输出） | `agent:agentic:` |
| POST `/agentic/authorized` | 用户对话页可用的编排应用：已发布 + 状态启用 + 授权角色命中当前用户 | `agent:agentic:` |
| POST `/agentic/logList`、`/agentic/logInfo` | 运行日志列表、详情（含步骤） | `agent:agentic:` |
| POST `/agentic/logDelete` | 删除运行日志 | `agent:agentic:delete` |
| POST `/agentic/chatList`、`/agentic/chatInfo` | 对话历史列表、详情（消息 + 每轮运行记录） | `agent:agentic:` |
| POST `/agentic/chatDelete` | 删除会话（连同消息与运行日志） | `agent:agentic:delete` |
| POST `/agentic/chatFeedback` | 消息反馈：点赞/点踩（可附标签与说明），再次提交同一情绪表示取消 | `agent:agentic:` |
| POST `/agentic/statistic` | 流程统计：会话数量与对话轮次时间轴、流程排名、用户排名 | `agent:agentic:` |

#### 流程统计

- 会话数量取会话表（按创建时间）、对话轮次取运行日志（一轮对话一条），两侧共用
  「流程 + 用户 + 对话类型 + 对话状态」四个条件，两张时间轴看的是同一批对话；
- 入参：`beginTime` / `endTime`（毫秒，默认近一周）、`agenticId`、`uid`、
  `type`（draft 调试运行 / published 发布应用）、`deleted`（only 已删除 / without 未删除 / 空 全部）、
  `status`（success 成功 / failed 失败 / 空 全部，按执行状态筛选）、
  `aggregation`（hour / day / week / month，默认 day，周取周一、小时取整点，按东八区切分）；
- 时间切分固定用东八区（与 `sys.datetime` 同一口径），服务所在时区（容器常见为 UTC）不影响分桶；
  周桶的键是周号（`2026-W39`），天/小时/月的键分别是 `2026-09-28` / `2026-09-28 10:00` / `2026-09`；
  区间内的空桶会补齐 0，趋势图不会因为没数据的时段而断线；
- 区间上限：整体 1 年，按小时 31 天（745 个桶）——明细是全量取回在内存里聚合的，超限返回 `1002`；
- 执行状态：轮次看运行日志自身的状态（1 成功 / 2 失败），会话看它自己的轮次——
  有一轮失败即为失败，全成功才算成功；会话状态由轮次推导，所以状态筛选不落到 SQL
  （先取回全量轮次再在内存里筛），筛选只影响统计结果，不影响会话状态的判定；
- 返回：`summary`（会话数量、对话轮次、参与用户、覆盖流程、成功率、平均耗时）、
  `timeline`（每个时间桶的会话数量 / 对话轮次 / 参与用户 / 覆盖流程 / 成功失败轮次 / 成功失败会话，键升序）、
  `types`（按对话类型的会话数量与对话轮次）、
  `statuses`（执行状态分布：成功 / 失败的会话数量与对话轮次，
  **不含执行状态筛选**——先记分布再按筛选决定算不算数，筛「失败」时也能看到两边的盘面）、
  `flows`（流程排名：会话数量、对话轮次、参与用户、成功率、平均耗时、最近对话）、
  `users`（用户排名：会话数量、对话轮次、覆盖流程、成功率、最近对话）；
- 排名按会话数量降序、轮次数量次之；明细经 MyBatis（`AgenticStatisticMapper`）按条件取回，
  只取聚合需要的列（不读标题、入参、输出、步骤等大字段），分桶、排名与去重都在服务层内存里完成
  ——与 lm 的用量统计同一套做法：SQL 不写数据库方言的日期函数，也不必为每种条件组合各写一条查询。

多轮对话：`run` / `invoke` 支持传 `chatId`（0 或空表示新建），会话落在 `fs_agent_agentic_chat` 与
`fs_agent_agentic_dialog`，类型区分 `published`（发布应用）与 `draft`（调试运行），且只能续写
「同一编排 + 同一类型 + 同一用户」的会话；历史消息作为模型上下文传给大模型节点，
返回结果里回带 `chatId`，下次带同一 `chatId` 即可继续对话。运行日志带 `chatId`，按会话可回看每轮的节点与工具明细。
`run` / `invoke` 还会回带本轮消息标识 `questionId` / `answerId` / `leafId`，调试面板可据此对回复直接反馈。
`chatInfo` 的消息里会带上本轮的运行日志标识 `logId`（按日志输出里的 `answerId` 关联），
前端据此按需拉取该轮的完整节点与工具调用过程（`logInfo`），用于定位与追踪问题。

#### 对话分支（编辑提问 / 重新生成）

消息按 `parent_id` 组成一棵消息树：`0` 表示分支起点，其余指向上一轮消息；会话的 `leaf_id` 记录当前分支尾，
模型上下文按 `leaf_id` 沿 `parent_id` 回溯取（`chatInfo` 会返回每条消息的 `parentId` 与会话的 `leafId`，
前端据此还原消息树并做分支切换）。两个可选入参驱动分支：

| 入参 | 取值 | 说明 |
| --- | --- | --- |
| `parentId` | 整数，可不传 | 从哪条消息往下续写；不传表示接在会话当前分支尾，显式传 0 表示从会话起点新起分支（编辑第一条提问重新发送） |
| `reuseQuestion` | true / false，默认 false | 重新生成：不重复落用户消息，只在 `parentId` 指向的用户消息下新增一条助手回复 |

编辑提问时带 `parentId = 被编辑消息的 parentId`（普通提问，落用户消息 + 助手回复）；
重新生成时带 `parentId = 被重新生成的助手回复对应的用户消息 id` 与 `reuseQuestion = true`。
两种方式都会新起分支并更新会话的 `leafId`，原分支的消息与运行日志都保留。

### 授权角色

- 编排应用带授权角色字段 `roleIds`（`fs_agent_agentic.role_ids`，逗号分隔存储，设计器画布属性里维护）；
  **为空表示所有登录用户可用**，配置后要求调用人的角色与之有交集；
  列表接口会按 `roleIds` 填充 `roles`（角色 `id` 与 `name`），前端列表页展示授权角色列，未配置时展示「全部用户」；
- 生效范围：`/agentic/invoke`（外部调用）与 `/agentic/run`（调试运行）都会先校验，未命中返回 `9403` 无权限；
  `/agentic/authorized` 同样按该规则筛选，用户对话页只列出可用的已发布应用；
- 存量库需要加列：`ALTER TABLE fs_agent_agentic ADD COLUMN role_ids varchar(2048) NOT NULL DEFAULT '' COMMENT '授权角色，为空表示所有登录用户可用' AFTER tags;`
- 运行日志与对话历史为**标记删除**（会话、消息、运行日志记 `deleted_time` / `deleted_uid`，不物理删除；
  会话列表支持按删除状态筛选：不传或 0 只看未删除、1 只看已删除、-1 全部），
  运行日志的编排名称不再冗余存库，返回时按 `agenticId` 关联填充；存量库调整：
  `ALTER TABLE fs_agent_agentic_log DROP COLUMN agentic_name,
   ADD COLUMN deleted_time bigint NOT NULL DEFAULT 0 COMMENT '删除时间，0 表示未删除（标记删除）',
   ADD COLUMN deleted_uid int NOT NULL DEFAULT 0;`

### 记忆（多轮上下文）

记忆按「节点自身的配置」逐节点生效：节点之间不共享历史、也不互相读取对方的输入输出；需要上游数据时一律用
`{{#节点标识.变量名#}}` 显式引用（唯一例外是「输出图表」节点，见上文——它按职责读取本轮的用户问题与直连大语言模型的回复、工具调用）。

节点配置 `memory` 的四个字段（`fs_agent_agentic_dialog` 里每轮存一条用户消息与一条助手回复，记忆从这里取）：

| 字段 | 取值 | 说明 |
| --- | --- | --- |
| `enabled` | true / false | 是否启用记忆；关闭即不带任何历史 |
| `scope` | `conversation`（默认）/ `user` | 记忆范围：完整对话 / 仅用户提问 |
| `window` | 整数（默认 10） | 记忆窗口，按「轮」计 |
| `toolchain` | true / false | 仅 `conversation` 有效：历史轮次里的工具调用（参数 + 返回结果）一并加入上下文 |

- `conversation`：取最近 `window` 轮，一轮 = 用户消息 + 助手回复（即最近 `window × 2` 条）；
  开启 `toolchain` 时，带工具调用的助手回复会还原成 `assistant(tool_calls)` + `tool(返回结果)` 的消息序列，
  工具返回内容按其原始 JSON 文本给出，失败时给出失败原因（便于模型下一轮自行修正参数）；
- `user`：只取最近 `window` 条用户提问，适合只需理解「用户问了什么」的节点；
- 未配置 `memory` 字段的模型节点不带历史（不产生隐式上下文）；上下文不做长度截断，记忆窗口是唯一口径；
- 各节点默认值：大语言模型 = 完整对话（`conversation`，窗口 10）；问题分类器、参数提取器、输出图表 = 仅用户提问（`user`，窗口 10）。
  需要「按上一轮的数据继续画图」这类跨轮追问时，把对应节点的记忆范围改成完整对话并开启工具链即可；
- 兼容：老数据没有 `scope` 字段时按 `conversation` 处理（保持升级前的行为），设计器属性面板会补上默认值。

入参校验：开始节点里声明为必填的自定义参数若未传值，`run` / `invoke` 返回 `1004` 与缺失参数名。
校验只在新建会话时执行（`chatId` 为空或会话不存在），继续对话的入参已随会话带入，不再重复要求。

工具调用：大模型节点按 `function calling` 循环执行配置的工具方法（发布内容用方法快照，草稿运行按工具标识实时解析），
被「执行变量」手工指定的参数不再暴露给模型、调用时以绑定值覆盖；每次工具调用的方法名、参数、返回结果与耗时都记在步骤的
`calls` 里，设计器调试面板与运行日志详情可直接查看。

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
| POST `/maintain/reindexChunk` | 重建检索块索引（SSE 输出进度，可传 `knowledgeId` 限定知识库） | `agent:maintain:reindexChunk` |
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
  `knowledgeId` 可省略：省略时按图片自身归属的知识库逐个判权（聊天历史等无知识库上下文的场景），
  授权变更在下一次渲染即时生效，历史消息里的图片同步不可见。
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

#### 工具方法

- 工具配置保存（或重新解析）时按类型解析出**方法清单**并落库缓存：`schema` 用 swagger-parser 完整解析 OpenAPI
  文档（JSON / YAML，`$ref`、`allOf` 一并展开），`mcp` 取同步结果里的 `tools`。
- 方法行存于 `fs_agent_tool_method`，业务唯一键 `tool_id + name`：重解析按该键 upsert，本次未出现的方法标记
  `present = 0`（软失效），人工改过的 `sort`、`status` 不覆盖；工具删除时方法一并清理。
- 参数只保留**方法级参数**（OpenAPI 的 `parameters` / `requestBody`，MCP 的 `inputSchema.properties`，含
  `name / type / required / description / in / defaultValue / enum`）；工具级 `url` / `header` / `query`
  只在后端调用与测试时使用，不进入方法参数，也不进入给模型的 tool 定义。
- 每个参数带 `schema`（对象参数含 `properties` / `required`）：设计器按字段逐个绑定执行变量。
  参数绑定为 `{ auto: true }`（由模型决定）或 `{ auto: false, value }`（手工指定内容，内容里可混排
  固定字符串与 `{{#节点标识.变量名#}}` 占位符，由运行时解析）；对象参数存
  `{ source: 'fields', fields: { 字段名: 绑定 } }`，未配置的字段仍由模型决定。
  发布校验会检查 `auto=false` 的参数是否填了内容。
- 方法名规范化为合法函数名（`[0-9a-zA-Z_-]`，超长截断并加哈希后缀），原始名记在 `origin_name` 供调用与排查；
  智能体编排按 `toolId + 方法名` 引用，方法失效时提示重新选择，不静默换方法。
- 编排发布（`/agentic/publish`）时会校验引用的工具与方法：工具存在且启用、方法存在且未失效未停用、
  执行变量绑定（引用变量 / 固定值）有取值；校验通过后把方法定义（名称、描述、参数、调用信息）快照进发布内容，
  工具后续变动不影响已发布编排。
- 技能与技能版本：维护技能基本信息与版本包，版本包上传至文件服务。
- MCP 服务：以 `spring-ai-mcp-server-webmvc` 暴露本地工具（`DemoTool`、`BiTool`），
  供模型通过模型上下文协议调用。

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST `/tool/list`、`/tool/info`、`/tool/config`、`/tool/mcpSync` | 工具列表、详情、下拉数据、MCP 探测 | `agent:tool:` |
| POST `/tool/save`、`/tool/delete` | 保存、删除工具 | `agent:tool:add`、`agent:tool:modify`、`agent:tool:delete` |
| POST `/tool/methods` | 方法清单：`{ id }` 或 `{ ids: [] }`，读库缓存 | `agent:tool:` |
| POST `/tool/parse` | 解析预览：解析未保存的配置返回方法清单，不落库 | `agent:tool:` |
| POST `/tool/parseSource` | 重新解析：按已保存配置重解析并落库 | `agent:tool:add`、`agent:tool:modify` |
| POST `/tool/test` | 方法测试：按配置真正发起一次调用（MCP `tools/call` 或 OpenAPI 请求） | `agent:tool:` |
| POST `/tool/methodSave` | 方法启用 / 停用与排序（描述与参数由解析结果决定） | `agent:tool:add`、`agent:tool:modify` |
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
| `/agent/agentic/statistic` | 流程统计（会话数量与对话轮次时间轴、流程排名、用户排名） |
| `/agent/chat/demo`、`/agent/chat/dialog`、`/agent/chat/compare` | 模型调试、模型对话、模型对比 |
| `/agent/knowledge/list`、`/agent/knowledge/document`、`/agent/knowledge/segment`、`/agent/knowledge/recall` | 知识库管理 |
| `/agent/plugin/tool`、`/agent/plugin/mcp`、`/agent/plugin/skill`、`/agent/plugin/skillVersion` | 插件管理 |

## 部署配置

| 配置项 | 说明 |
| --- | --- |
| `spring.datasource.agent.*` | 数据源：url、username、password、`table-prefix=fs_agent_` |
| `rpc.agent.name`、`rpc.agent.rest` | 服务注册信息，管理端 `/api/agent/**` 按 `rpc.agent.rest` 转发 |
| `rpc.lm.rest` | 模型网关地址，对话转发、词嵌入、重排经此调用 |
| `fs.agent.token` | 访问模型网关的认证密钥，整个 agent 服务共用（大语言模型等节点、模型对话、词嵌入均使用） |
| `fs.agent.image.expire` | 原图地址签发有效期（毫秒），默认 1800000 |
| `rpc.file.rest` | 文件服务地址，图片上传与地址签发经此调用 |
| `fs.format.date` | 日期格式 |

服务端口默认 `7827`，服务名 `fs-agent-service`，
JPA 表前缀策略为 `com.iisquare.fs.web.agent.dsconfig.NamingStrategy`。

## 数据与存储

- MySQL：`fs_agent_agentic`（编排草稿与发布内容）、`fs_agent_agentic_chat`（会话）、
  `fs_agent_agentic_dialog`（会话消息）、`fs_agent_agentic_log`（运行日志）、
  `fs_agent_knowledge`、`fs_agent_knowledge_chunk`、`fs_agent_knowledge_document`、`fs_agent_knowledge_image`、
  `fs_agent_knowledge_segment`、`fs_agent_skill`、`fs_agent_skill_version`、`fs_agent_tool`，
  建表语句见 `docs/fs_project_agent.sql`，存量库改造见 `docs/fs_project_agent_migrate.sql`。
- 流程统计的明细查询走 MyBatis（`mapper/AgenticStatisticMapper.xml`），与 JPA 共用同一数据源
  （`spring.datasource.agent`，即 `@Primary` 的 `agentDataSource`）。
- Elasticsearch：检索块集合 `fs_lm_knowledge_chunk`。
- 文件服务：桶 `fs-lm-knowledge`、`fs-lm-skill`。

#### 会话表结构说明

- `fs_agent_agentic_chat`：`agentic_id` 归属编排（列表按编排直查，不再子查询运行日志）、`leaf_id` 当前分支尾、
  `deleted_time` / `deleted_uid` 标记删除与删除人；不存删除原因与描述。
- `fs_agent_agentic_dialog`：`parent_id` 消息树父节点、`reference` 工具调用明细与图表、
  `feedback_*` 点赞点踩；不存意图识别、结束原因与审核字段——会话审核若要做成闭环，
  另建审核表留痕（多次审核）比在消息行上放单值列更合适。
- `fs_agent_tool.content_hash` / `parse_status` 已删除：标注为「解析缓存 / 解析状态」用，
  但保存与手动重解析都会真实重解析，页面提示读的是 `parse_error`，两者从未被读取。
