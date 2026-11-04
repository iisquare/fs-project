# 多 Agent 方案调研与取舍

> 记录日期：2026-11-04  
> 关联运行说明：[multi-agent-trial.md](./multi-agent-trial.md)

## 1. 调研对象

选型参考了三个当前客户端类产品：

| 产品                                                                                                                  | 调研重点                                                                                 | 对方案的启发                                                          |
| --------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| [OpenAI Codex](https://learn.chatgpt.com/docs/agent-configuration/subagents)                                          | 主线程派生专业子线程；主线程等待并汇总；子 Agent 继承沙箱和审批边界；跨任务记忆独立存在  | 子 Agent 应是有限职责的独立线程，结果回主线程，不让全部过程污染主会话 |
| [OpenCode](https://opencode.ai/v2/docs/agents/)                                                                       | Primary / Subagent 分离；父子 Session；支持前台、后台、完成后通知和显式 `sessionID` 续接 | Agent 定义与执行 Session 必须分开：同名 Agent 不等于自动继承旧上下文  |
| [WorkBuddy](https://www.workbuddy.cn/docs/workbuddy/From-Beginner-to-Expert-Guide/Function-Description/Expert-Center) | 专家角色、专家团团长拆解与并行、任务级上下文、长期记忆、权限和 Worktree 隔离             | 专业角色可复用，但业务任务应有独立边界；写操作需要资源和工作目录隔离  |

补充参考：

- Codex [Memories](https://learn.chatgpt.com/docs/customization/memories)：跨任务经验应经过筛选，不是复制完整聊天记录。
- Codex [Auto-review](https://learn.chatgpt.com/docs/sandboxing/auto-review)：权限审批可以由独立窄职责 Agent 处理，但仍受确定性沙箱约束。
- OpenCode [Session Contract](https://github.com/anomalyco/opencode/blob/dev/specs/v2/session.md)：同一 Session 串行、不同 Session 并发，使用持久化事件和检查点恢复。
- WorkBuddy [Worktree Task](https://www.workbuddy.cn/docs/workbuddy/From-Beginner-to-Expert-Guide/Function-Description/Worktree-Task)：并行写任务需要独立工作目录。

共同结论：

> 主流实现不是“Agent 自由协商”，而是中心协调、独立子会话、结构化摘要、权限继承和受控并发。

## 2. 场景适用性

| 场景                                    | 适合的方案                                 |
| --------------------------------------- | ------------------------------------------ |
| 单文件修改、短命令、连续依赖的问答      | 单 Agent 更直接                            |
| 网页浏览、反复填写、登录辅助、页面提取  | 独立 Browser Agent 收益明显                |
| 知识库、数据主题、业务方法组合查询      | 合并为一个 Project Tool Agent 更合适       |
| 多个互不依赖的查询或分析                | 子 Agent 并行                              |
| 多个 Agent 同时写文件或操作同一业务账号 | 必须资源串行，或使用独立 Worktree / 资源域 |
| 完全自由的 Agent 互相委派               | 不采用；循环、权限扩散和成本不可控         |

## 3. 工具拆分与合并

### Browser Agent

适合独立：

- 页面快照和控件列表很长，且每次动作后都会重复返回。
- 一个网页任务通常需要多轮观察和操作。
- 主 Agent 真正需要的通常只是最终状态、关键信息和交付物。

最终边界：

- 只持有 `browser_op`。
- 使用独立子 Session 和独立标签页集合。
- 原始快照、点击过程和下载记录留在子会话。
- 上传本机文件仍经过项目工作区权限和审批。

### Project Tool Agent

适合合并：

- 项目自定义工具经常组合出现，例如先查知识库、再查数据主题、最后调用业务方法。
- 如果每个工具一个 Agent，会产生大量无意义转交。

最终边界：

- 持有项目配置中除技能外的全部自定义工具。
- 工具方法、知识库、数据主题、知识图谱和编排应用共用一个专业 Agent。
- 工具返回和中间参数留在子会话，只回传摘要。

### 主 Agent 保留

- 文件与命令：与工作区状态强相关，暂不拆分。
- 本地技能：依赖本机文件、命令和访问策略。
- 项目长期记忆与定时任务：属于用户级或项目级操作。
- 最终需求判断、结果整合和用户回复。

### 不采用逐工具拆分

逐工具 Agent 会让“工具数量”转化为“Agent 数量”，增加：

- 专业角色选择成本。
- 子任务往返延迟。
- 上下文重复和 token 成本。
- 结果合并与失败恢复复杂度。

## 4. 对上下文的影响

### 拆分后的收益

- 浏览器快照不再持续占用主会话。
- 业务工具返回和组合调用链不再直接进入主会话。
- 主 Agent 只保留任务、决策和最终摘要。
- 主上下文可以更长时间保持稳定。

### 新增的成本

- 每次委派增加一次父子 Agent 交接。
- 子 Agent 需要独立理解和执行任务。
- 多层 Agent 会增加 token、延迟和失败点。

因此限制：

- 子 Agent 返回主 Agent 的摘要最多 16 KiB。
- 子 Agent 最多 24 个工具往返。
- 持久化只保留用户任务和最终回答，不保存完整中间工具链。
- 续接最多加载 16 条高层问答或 60 KiB 历史。

### 状态策略

```text
Agent 配置       持久
任务 Session     任务内持久，默认新开
完整工具过程     只存在于当前执行
跨任务经验       经过筛选、按项目和 Agent 隔离
```

同名专业 Agent 不会自动继承上次完整对话。继续同一任务时，使用显式 `session_id` 或稳定 `task_key`。

## 5. 最终实现

```text
主 Agent
  → delegate_agent
  → 资源感知调度器
  → Browser Agent / Project Tool Agent
  → 独立 Session 和受限工具集
  → 摘要返回主 Agent
```

最终入口：

```text
delegate_agent({
  agent: "browser" | "project_tools",
  objective,
  context?,
  session_mode?: "new" | "auto" | "reuse",
  session_id?,
  task_key?,
  remember?
})
```

关键规则：

- 默认新会话；显式会话和 `task_key` 才能受控续接。
- 全局最多 3 个子 Agent。
- Browser Agent 按用户串行，Project Tool Agent 按项目串行。
- 不同资源可以并行，同资源不允许并发写入。
- 状态包括等待、运行、等待用户、完成、失败、停止和中断。
- 30 秒心跳；超过 2 分钟无心跳标记中断。
- 父会话停止会取消全部子 Agent；单任务停止只取消该实例。
- 删除或清空父对话会删除对应子会话。
- 跨任务经验默认低信任，并且可以清空。

## 6. 实现位置

| 内容                            | 文件                                                                                      |
| ------------------------------- | ----------------------------------------------------------------------------------------- |
| 主 Agent 工具与统一委派入口     | `src/renderer/src/core/Toolbox.ts`                                                        |
| 子 Agent 调度、执行、会话与恢复 | `src/renderer/src/core/AgentRuntime.ts`                                                   |
| 状态和交互界面                  | `src/renderer/src/stores/agents.ts`、`src/renderer/src/components/Chat/AgentActivity.vue` |
| 子 Agent 数据层                 | `src/renderer/src/db/AgentDb.ts`                                                          |
| 浏览器标签页与资源隔离          | `src/main/automation.ts`                                                                  |
| 子 Agent 数据库结构             | `schema-v14.sql`、`migrations-v13.sql`、`migrations-v14.sql`                              |

## 7. 验证与限制

已验证：

- TypeScript 类型检查和完整生产构建通过。
- v12 → v13 → v14 数据库升级链通过。
- 实际开发用户库迁移成功，二次启动完整性检查通过。
- 应用启动冒烟测试通过。

当前限制：

- 尚未完成真实登录模型下的多 Agent 端到端评测。
- `waiting_user` 根据子 Agent 最终结论识别，不是浏览器中途暂停。
- 专业 Agent 目前继承项目模型，尚无独立模型配置 UI。
- 跨任务经验按最近记录注入，尚未做语义相关性排序。
- 多客户端操作同一 Session 时，尚无数据库级分布式租约。
