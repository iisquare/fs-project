# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`fs-bot` is an Electron desktop application built with Vue 3 and TypeScript. It was scaffolded from the `electron-vite` quick start template.

## Package Manager

This project uses **pnpm**. All scripts and install commands must use `pnpm`, not npm or yarn.

## Commands

| Command | Description |
|---------|-------------|
| `pnpm install` | Install dependencies |
| `pnpm dev` | Start development with HMR |
| `pnpm start` | Preview the built app |
| `pnpm build` | Typecheck and build for production |
| `pnpm format` | Format all files with Prettier |
| `pnpm lint` | Lint all files with ESLint (cached) |
| `pnpm typecheck` | Run both node and web typechecks |
| `pnpm typecheck:node` | Typecheck main + preload processes |
| `pnpm typecheck:web` | Typecheck renderer (Vue) process |
| `pnpm build:win` | Build Windows installer |
| `pnpm build:mac` | Build macOS app |
| `pnpm build:linux` | Build Linux packages |
| `pnpm build:unpack` | Build unpacked (no installer) |

## Architecture

The project follows the **electron-vite** three-part structure:

```
src/
  main/       → Electron main process (Node.js)
  preload/    → Preload script (context bridge)
  renderer/   → Vue 3 app (browser window)
```

### Main Process (`src/main/index.ts`)

Creates a single `BrowserWindow`, handles app lifecycle (ready, activate, window-all-closed), and opens external links in the OS browser. Uses `@electron-toolkit/utils` for `electronApp`, `optimizer` (DevTools shortcuts), and `is` (dev/prod detection). IPC listeners are registered here via `ipcMain`.

### Preload (`src/preload/index.ts`)

Exposes `@electron-toolkit/preload`'s `electronAPI` and a custom `api` object to the renderer via `contextBridge`. The type declarations in `src/preload/index.d.ts` augment the global `Window` interface.

When adding new IPC channels, expose them through the `api` object here and update the type declarations.

### Renderer (`src/renderer/src/`)

A Vue 3 app using Composition API (`<script setup lang="ts">`). The `@renderer` alias resolves to `src/renderer/src/`. Static assets with `?asset` suffix are resolved by electron-vite.

**Element Plus** is configured for on-demand import via `unplugin-vue-components` and `unplugin-auto-import`. Components and APIs (`ElMessage`, `ElNotification`) are auto-imported. Icons from `@element-plus/icons-vue` must be imported explicitly. Auto-generated `auto-imports.d.ts` / `components.d.ts` are committed.

Root component (`App.vue`) calls `user.initialize()` on mount (restores auth from localStorage) and wraps `<router-view />` in `<el-config-provider locale="zh-cn">`.

#### Router (`src/renderer/src/router/`)

Uses `createWebHashHistory` — `#/path` hash format for Electron's `file://` protocol.

- `config.ts` — Path constants (`page.home`, `page.login`, `page.e404`)
- `index.ts` — Route definitions and `beforeEach` guard

**Route guard** follows the fs-website pattern: only checks login state for `/user/*` routes. No RBAC or permission checking — unprotected pages render freely, `/user/` routes (except `/user/login`) redirect to login when `!user.isLogined()`.

#### Stores (`src/renderer/src/stores/`)

Pinia setup stores (Composition API):

- `counter.ts` — `useCounterStore`: app-level flags (`routing`, `fetching`, `count`)
- `user.ts` — `useUserStore`: identity with localStorage token persistence (`STORAGE_KEY = 'fs_auth_token'`). Key methods: `initialize()` (restore on app start), `reload()` (validate token with API), `reset(data, isReady)`, `isLogined()` (`id > 0 && token !== ''`), `visible` flag for login-dialog trigger on `90401` response.

#### API (`src/renderer/src/api/`)

Axios wrapper pattern (adapted from fs-website):

- `Api.ts` — Core: `request()` sets `baseURL` + `withCredentials: true` + `Authorization: Bearer <token>`. `fetch()` handles responses via `ApiUtil` (code/message/data extraction). Code `90401` sets `user.visible = true`. Exports `get()` / `post()`.
- `ApiUtil.ts` — Response helpers: `succeed()`, `code()`, `message()`, `data()`, `result()`
- `UserApi.ts` — Example module: `login()`, `logout()`, `info()` endpoints

#### Data Layer (`src/renderer/src/core/`, `src/renderer/src/db/`)

User data and system config are stored separately, each with its own bridge module:

- **运行目录（runtime dir）**：`<运行目录>` 是配置、用户库、图标与技能这四份本地数据的根。开发模式 = 项目根目录（`userdata/`、`skills/` 已在 `.gitignore` 里），生产环境 = **用户主目录下的 `.fs-bot`**（如 `C:\Users\<用户>\.fs-bot`）。它只在 `main/index.ts` 里算一次（`is.dev ? app.getAppPath() : join(app.getPath('home'), '.fs-bot')`），再传给 `config.init` / `skills.init` / `initIcons` / `db.initUser`，各模块只拼自己的子目录（`userdata/config.toml`、`userdata/<userId>/fs-bot.db`、`userdata/icons/`、`skills/`）。**不往安装目录里写**：Windows 的 Program Files 未必可写，macOS 的「安装目录」更是 .app 包内部——往包里写会破坏代码签名，应用更新换整包时数据也会跟着丢。
- **User data (SQL)** — per-user encrypted SQLite at `userdata/<userId>/fs-bot.db` (SQLCipher + WAL, integrity hash in `_integrity`). `core/Db.ts` is the IPC bridge; `db/<Domain>Db.ts` wraps it per table (`ConversationDb`, `ProjectDb`, `TaskDb`) and returns `ApiUtil` result envelopes. 桥接层除增删改查外还给一个**关键词检索**（`Db.search` → `DatabaseManager.search`：表 + 列清单 + 关键词，可加等值 / 集合条件与 `orderBy` / `limit`；只用于「按内容找行」的查询，例如历史对话检索——几百上千条消息交给 SQLite 一条语句查完，比整表拉回渲染进程再过滤干净得多）。`db/` is reserved for SQL — do not put file-backed accessors here.
- **User DB schema versions** — one SQL file pair per version, flat in `src/main/database/sql/`: `schema-vN.sql` is the complete schema used for new databases, `migrations-vN.sql` is the incremental upgrade script applied to existing ones. Register both in `schema.ts` (`USER_SCHEMAS`) and `migrations.ts` (`USER_MIGRATIONS`); `USER_SCHEMA_VERSION` is derived from the registry. `ensureUserSchema()` runs either path inside a single `SAVEPOINT`, refuses downgrades, and fails loudly on a missing script. Conventions: `sql/README.md`; operation / upgrade / rollback runbook: `scripts/README.md` (`pnpm schema version|key|export`).
- **System config (TOML file)** — `userdata/config.toml`, owned by the main process (`src/main/config.ts`, parsed with `smol-toml`). It holds `settings` (values typed by `SETTINGS_SCHEMA`) and `loginHistory`. `core/Config.ts` is the IPC bridge; stores and views call it directly (`stores/settings.ts`, `views/login.vue`). Add a `config/` folder only if a domain layer for the config file becomes worthwhile.
- **本地图标（文件）** — `userdata/icons/`，主进程 `src/main/icons.ts` 负责：`icon:pick` 选图后复制进来并回传 `app-icon://<文件名>`，协议在 app ready 之前登记成标准协议（渲染端 `index.html` 的 `img-src` 已放行该协议，渲染进程拿不到图标目录之外的文件）。图标字段始终是一个地址字符串：网络地址手填、本地图片回填，两者在库里同字段。

### Build & Config

- **`electron.vite.config.ts`** — Defines Vite configurations for main, preload, and renderer. The renderer config includes the Vue plugin and `@renderer` path alias.
- **`tsconfig.json`** — Root config using TypeScript project references to `tsconfig.node.json` and `tsconfig.web.json`.
- **`tsconfig.node.json`** — Extends `@electron-toolkit/tsconfig` for the main/preload Node.js target.
- **`tsconfig.web.json`** — Extends `@electron-toolkit/tsconfig` for the renderer browser target.
- **`electron-builder.yml`** — Packaging config for Windows (NSIS), macOS (DMG), and Linux (AppImage/snap/deb).

### Environment Variables

electron-vite loads `.env` files via Vite's built-in mechanism. Variables prefixed with `VITE_` are available via `import.meta.env` in **all three processes** (main, preload, renderer).

| File | When loaded |
|------|-------------|
| `.env.development` | `pnpm dev` |
| `.env.production` | `pnpm build` |

Local overrides (`.env.local`, `.env.*.local`) are gitignored and loaded last — use these for per-machine settings.

### Code Style

- Prettier: 100 char width, single quotes, no semicolons, no trailing commas
- ESLint: extends `@electron-toolkit/eslint-config-ts` + `eslint-plugin-vue` (flat/recommended)
- Console output must stay ASCII (no `→`, `—`, CJK): Windows consoles default to the GBK code page and garble UTF-8 output. Chinese is fine inside files (docs, `config.toml`), not on stdout/stderr.
- Vue SFC blocks must use `<script lang="ts">`
- `vue/multi-word-component-names` and `vue/require-default-prop` are disabled
- Styles: **SCSS** — use `@use` for shared partials (`_base.scss`), nesting for parent-selector patterns. Vue SFC `<style lang="scss">` is supported.

### UI Conventions

- **Create / edit forms stay on the page.** No `el-dialog` and no tabs hiding fields: every field of the form is laid out flat.
- **List and form are two independent views.** Each takes the full content area, and the form replaces the list instead of sitting next to it in a left/right split — a split squeezes the form as soon as the window gets narrow.
- **No second title on the form view.** The list view keeps the page header (标题 + 说明 + 新建按钮); the form view drops it and keeps only a slim **sticky** bar inside the card: `← 返回列表` on the left, that item's actions on the right — long forms stay escapable without scrolling back up. The record's name is already the first field of the form, so repeating it as a heading just adds chrome.
- Reference implementation: `src/renderer/src/views/project/` (`ProjectManagement.vue` list ⇄ `ProjectEditor.vue` form) and `src/renderer/src/views/schedule/ScheduledTasks.vue`.
- Dialogs are still fine for small transient actions (confirm, prompt, 调用测试 …), just not for data-entry forms.
- **侧边栏可以拖拽调整宽度**（`views/index.vue` + `stores/sidebar.ts`）：侧边栏与内容区由 `el-splitter` 分隔，拖分隔条即可改宽度（限 200–480px），松手落到 `localStorage`（`fs_sidebar_width`），下次打开还是这个宽度；分隔条本身（hover 高亮主色）就是与内容区的分界线，侧边栏不再自己画右边框。宽度按像素存（不是百分比），窗口缩放不会把宽度按比例漂走；`TitleBar` 的置顶按钮仍按原样整块收起 / 展开侧边栏。
- **Server-owned catalogs stay server-owned.** 应用（编排）、技能、知识库、工具方法、数据主题、知识图谱本体、模型清单都在服务端维护，桌面端不提供管理页面：只在项目里选择引用（见 `ProjectEditor.vue` 的「模型」与「工具」两段），本地只存「选中了哪些」。模型是「输入框 + 检索候选」（`el-autocomplete`，取值就是输入框文本，清单里没有的模型也能手填）；工具候选行与已添加的行都带一个帮助按钮，点开看服务端给出的描述（工具方法为方法描述，添加时随行保存）。
- **项目配置可以导入 / 导出，也可以发布到服务端分享**，分享不单独占一个入口：项目管理页（`views/project/ProjectManagement.vue`）里分「我的项目 / 我的分享 / 浏览分享」三个页签，列表与表单仍是「表单替换列表、占满内容区」那套（项目表单与分享表单各自一个视图）。文件格式在 `utils/ProjectUtil.ts` 里定义：`{ type: 'fs-bot-project', version, exportedAt, share, project }`，`share` 只在来自分享时为对象；读写都由主进程弹原生对话框完成（`main/projects.ts`，IPC `project:export` / `project:import`），渲染进程只给文件内容或拿回原文。导出的入口是项目编辑器底部的「导出配置」（导出当前表单，未保存的改动也一并导出），导入的入口是页头的「导入项目」（重名自动加「（导入）」后缀，来自分享的文件会在提示里写明来源）。分享按「项目 + 版本」发布到服务端（`fs_agent_project_share` / `fs_agent_project_version`，见 `components/Project/ShareList.vue` 与 `ProjectShareForm.vue`）：编辑器底部的「分享」先保存再打开分享表单，「我的分享」卡片上的「分享新版本」直接把目标锁到那个分享项目，表单里选本地项目、填版本号与版本说明即可发布；自己的分享可以继续追加版本、导出任一版本，浏览分享里可以导出别人的配置或直接创建成本地项目（服务端只给已启用且授权命中的项目与版本）。**版本号收在卡片右上角的下拉里，用的是公共组件 `components/Version/VersionSelect.vue`**（技能管理页与分享卡片共用，一处改样式两边都受益）：版本数据源由调用方以 `loader` 注入——分享卡片按页问 `/bot/project/versions` 要（检索走服务端的 `name` 参数，不在前端过滤已取回的那一页），技能卡片一次取回该技能的全部启用版本（检索在 loader 里本地过滤）；面板里有检索框、当前版本打「✓」、`applied` 的那一版标「已安装」，选中哪一版由调用方决定用途（分享卡片据它导出配置 / 创建本地项目，技能卡片据它下载安装）。列表与面板都按页取（项目每页 30、面板每页 10，面板自己限高滚动——卡片里不套内嵌滚动条，页面本身就在滚），服务端列表行的版本数量与最新版本走轻量查询（逐项 COUNT + 只取最新一条，不把配置原文拖回来）；技能清单（`/skill/available`）同样带上了 `versionCount` 与 `latestVersion`，技能卡片不必再为显示一个版本号把每个技能的版本都请求一遍。本地项目用 `projects.share_id` / `share_name` / `share_version` 记住分享去向（卡片上回显「已分享 v1.0」，再次分享默认选中同一个分享项目）——这三个字段只是本地便签，服务端才是分享内容的权威存储。**分享内容包含环境变量（含密钥）、长期记忆（`projects.memory`）与工作区目录路径**，发布前页面上有明确提示。
- **项目只有一条执行路径：模型。** 项目用 `model` + `contextLength` + `systemPrompt` + `memory` + `thinkEffort` + `tools` + `workspace` 描述自己，不绑定编排应用；对话由客户端经 `core/ModelChat` 调客户端专用的 `/bot/models`、`/bot/chat`（历史由客户端带上、会话与消息只落本地库；`systemPrompt`、`memory` 与 `thinkEffort` 按会话所属项目随每轮请求下发，留空则不发、由模型与网关取默认值）。
- **长期记忆单独占项目配置的一格（`projects.memory`，项目编辑器的「记忆」区）**：与系统提示词分开写、**由用户手工维护**——模型只读（随请求下发），没有任何写入通道，`memory` 工具查的是原始对话记录、不碰这一格。下发时 `stores/chat.ts` 的 `memoryBlock` 给它加上 `# 记忆` 标题，`systemPromptOf` 按「系统提示词 → 记忆 → 本机说明」合成同一条 system 消息（服务端不区分这三段）；项目里所有会话共用，改完保存后下一次提问生效（`ChatProfile.memory` 随 `chatProfile(project)` 一起走，编辑 / 重新生成那两条手搓 profile 的路径也各自带上）。它和系统提示词一样**每轮都占上下文**（计入用量圆环），所以编辑器里写明「只写长期有用的事实，别抄整段对话」。导出 / 分享 / 导入都随项目配置走（`utils/ProjectUtil.ts` 的 `ProjectConfig` 有 `memory`），发布前的提示语里已并列「记忆」：记忆里写了私密内容，分享出去就会一并带出。**schema v11** 加的这一列（`memory TEXT DEFAULT ''`）走常规两份文件 + 注册表，老库一律补空串、行为与升级前一致（不下发这一段）。
- **系统提示词后面还追加一段「本机」说明**（`WorkspaceManager.describe` → IPC `workspace:prompt` → `stores/chat.ts` 的 `systemPromptOf`），**只列事实、不写解释**：`# 环境信息` 下是操作系统与架构、命令用的 shell，`## 工作区` 下按行列出目录与权限（主目录另标），零目录时换成一句「未声明工作区目录：文件操作必须给绝对路径，执行命令会被拒绝」。这段随每轮请求下发、占的是上下文，所以加内容前先掂量值不值（相对路径与命令默认落在主目录这类口径已经在工具说明里写了，不重复）。它每轮重取（目录、主目录改了当轮生效），也计入上下文用量圆环，服务端把它与项目提示词合成同一条 system 消息。工具按类型添加（工具方法 / 知识库 / 数据主题 / 知识图谱 / 编排应用，与服务端编排里大语言模型节点的工具种类一致），凭据与调用都在服务端；编排应用只是其中一种工具类型。

### 对话（一轮 = 模型 → 工具 → 模型）

- **调用循环跑在客户端**（`stores/chat.ts`）：一轮对话里模型可以反复请求工具，客户端每次把工具结果接回上下文再让模型继续，直到模型给出最终回复。轮次上限不是硬终止：跑满一批 `MAX_ROUNDS` 轮仍在请求工具就自动接续下一批，接续点随请求补一条 `continuePrompt` 提示词（讲清已跑多少轮、还剩多少轮，点名无效轮转的风险，同时说明没有异常就照常推进、别为省轮数提前收尾；上下文不清空，且**只保留最新一条**，链里不会堆起多条催促）；接续 `MAX_ROUNDS_CONTINUE` 次后再做一次**不带工具的收尾调用**——工具往返 = `MAX_ROUNDS × (1 + MAX_ROUNDS_CONTINUE)`，收尾是额外加的一轮、**不占用批次数**（每一批都是完整的 `MAX_ROUNDS` 轮），让模型按已完成的进度给出结论，这一轮记 `done` 并给一条非错误提示（`session.notice`，可回复「继续」接着做），不再像过去那样判失败。会话记录里只留模型自己的产出，提示词不落库、不进后续轮次的上下文。会话历史由客户端维护，因此每轮的完整调用链（`assistant` 的 `tool_calls` + 逐条 `tool` 返回）都随下一次请求一起带上，模型据此续推；调用链随消息落在 `messages.reference`（JSON），历史会话打开后按同一形状还原。
- **工具由 `core/Toolbox.ts` 归一**：本地工具一共五个——`file_op`（文件与目录）、`run_command`（本机命令）、`schedule_task`（定时任务）、`memory`（历史对话 / 记忆）与 `local_skill`（本地技能）；文件与命令由主进程执行，定时任务、历史对话走本地库（渲染进程），本地技能的清单与文件由主进程读技能目录。项目工具（工具方法 / 知识库 / 数据主题 / 知识图谱本体 / 编排应用）的定义与执行都在服务端（`/bot/tools` 取定义、`/bot/tool` 执行一次），客户端只做函数名分派与调用链拼装。
- **同一实体的多个操作合成一个工具、用 `action` 参数区分**（`file_op` / `schedule_task` / `memory` / `local_skill`；`run_command` 单列，因为它的作用范围不限于工作目录、每次都要过访问策略，与文件操作不是一套口径）：每轮请求都要带上整份工具定义，一个实体拆成五个 function 光骨架就白占几百 token（本地工具从 19 个 function 收到 5 个）。代价是 JSON Schema 表达不了「哪个 action 要哪些参数」，所以 schema 里只把 `action` 标成必填（带 `enum`），条件必填与取值校验放到执行期——`actionOf` 先核一遍 action（不在清单里就把可选项列出来），各分支再校验自己缺什么（`file_op` 用局部 `need`，如 `read` 要 `path`、`move` 要 `from` / `to`；`write` 的 `content` 允许空串，只认「没给」），错误信息照旧作为调用失败回到模型，模型据此补齐后重试。**工具名取产品语言，泛称的歧义由描述第一句兜住**：这组能力对外就叫「记忆」，所以工具名是 `memory`；但 `memory` 也常被理解成「可以写入的长期记忆」，所以描述第一句写死「只读地翻本机历史对话」，action 里也没有写入口。
  - `file_op` 的 `action` 取 `read` / `write` / `list` / `mkdir` / `move` / `delete` / `search` / `upload`：前七个是原来那批文件与目录工具，`upload` 是原来的 `upload_file`（上传到文件服务拿 `id`，服务端工具方法按 `id` 引用本机文件）；路径口径、主目录优先、越界走访问策略这些都没变。
  - 定时任务 `schedule_task`：`action` 取 `list` / `create` / `update` / `delete` / `run`。走 `stores/schedule.ts`，与「定时任务」页面共用同一份数据（模型改完页面立刻刷新，两边读的都是 `tasks` 表）；cron 落库前按页面那套口径校验（5 段：分 时 日 月 周），没给 `project_id` 时默认当前对话所属的项目，`run` 真的发起一轮后台对话（等于页面上的「运行一次」，会花掉一次模型调用）。
  - 历史对话（记忆）`memory`：`action=list` 按时间倒序列出范围内的会话（标题、时间与会话 id，先看有哪些对话）；`action=search` 按关键词查本地库的**会话标题与消息正文**，命中给会话 id、标题、角色、时间、消息在会话里的**条号**（`第 n/共 m 条`）与关键词附近的片段——条号就是 `read` 的 `offset + 1`，模型据此一步跳到命中处附近，不必从头翻；`action=read` 按会话 id 读消息正文（角色 + 时间 + 工具调用次数，按 `offset` 分段落读，会话里若留有编辑 / 重新生成产生的**旧分支消息**会在开头点明，免得模型把分支当成一条线）。`list` 与 `search` **默认只覆盖当前对话所属的项目**（`scope: all` 才跨项目；会话本身没有所属项目时退回全部项目并在结果里写明范围）。检索由主进程一条 SQL 查完（`db:search` → `DatabaseManager.search`：`columns` 逐个 LIKE、可加等值（`where`）与集合（`in`）条件、`orderBy` / `limit`；关键词与取值都走占位符，`%` / `_` / `\` 按字面量转义），不把整表拉回渲染进程过滤；跨项目检索时把会话清单按上限截断，兜住 IN 列表的长度；命中条号按会话逐个算（同一个会话只取一次消息清单）。
  - 本地技能 `local_skill`：`action` 取 `list` / `files` / `read`，读本地 `skills` 目录（服务端技能包解压出来的那份，见 `main/skills.ts`）。`list` 每个技能带一行**摘要**（`SKILL.md` frontmatter 的 `description`，支持 `|` / `>` 块写法；没有就取第一段正文并跳过 frontmatter），模型看这行就能挑技能、不必逐个读说明；`read` 省略 `path` 就是读 `SKILL.md` —— 技能的主入口，模型照它做事，再按 `files` 给的相对路径读脚本与参考文档；相对路径由主进程限制在技能目录内（技能包内容按不可信输入处理），非文本文件（前 8KB 里有 NUL）直接拒绝、单文件超 256KB 按上限截断。技能里的脚本要跑仍走 `run_command`（因此照样过一遍本地访问策略，`skills` 目录通常不在项目工作区里，会按「工作区外」申请），`@skill:名称` 那条路（把说明正文引用进上下文）不变。
- **本机文件交给服务端工具方法要先上传（`file_op` 的 `action=upload` 与 `/upload`）**：服务端读不到客户端的磁盘，本机路径传给 `multimodalAnalyze` 这类工具方法会被拒绝（服务端只认文件内容、可访问地址或文件服务的 `id`）。本地文件走「主进程读内容 → multipart 直传 `/bot/upload` → 拿回 `{ id, name, type, suffix, size }` → 模型按 `id` 引用」这条路：模型可以自己调本地工具 `file_op`（`action=upload`，它能从用户那句话里拿到路径），用户也可以在输入框里用 `/upload` 选文件、把文件标识等信息写进消息；两条路共用同一个上传通道与同一份本地访问策略（读取没被放行就不会上传）。
- **工具方法可以把参数固定下来**（`ProjectEditor.vue` 的「参数」 → `db/ProjectDb.ts` 的 `ProjectTool.args`）：某个参数切成「固定」（对象参数按字段逐个配）后，取值随项目落库，跑起来时由服务端补齐 —— `/bot/tools`、`/bot/tool` 把项目里的工具引用原样交给工具实现（`BotService.item` 透传 `args`），因此定义里会自动摘掉这些参数（模型不必再生成，也不会与配置打架）、调用时按取值覆盖（对象参数只覆盖同名的那几个字段，其余仍用模型给的，取值按文本下发）。这套绑定与编排里大语言模型节点的执行变量是同一个实现（`AgenticTool.definition` + `MethodTool.invoke`）。面板渲染期按需补出「自动 / 空值」的默认绑定，保存前统一整理成「只留固定项」（留空视为不固定）。
- **工作区权限是主进程的事**：项目声明的工作区目录按各自的读 / 写权限放行；**工作区之外**的路径按访问策略处理（`workspacePolicy`，`userData/config.toml` + 系统设置页面）：`ask` 请求批准（主进程推 `workspace:approval-request` 给界面，答复走 `workspace:approval:response`）、`auto` 帮我批准、`deny` 自动拒绝、`full` 完全访问权限。策略的判定与执行都在主进程，渲染进程改不了结论；被拒绝时失败原因作为工具返回交给模型，本轮对话不中断。**命令不适用「落在授权目录内就放行」这条**（见下一条）。
- **清单里可以标一个主目录**（`workspace[].primary`，项目编辑器「工作区」一节点「设为主目录」，至多一个）：命令省略 `cwd` 时在它里面跑，相对路径按「主目录优先、其余按声明顺序」解析——先取已经存在的那一份，都没有就落到主目录，新建文件因此默认落在主目录里。没标主目录的旧配置与分享来的配置一律按第一个目录，行为与从前一致。取用只走 `WorkspaceManager.primaryOf` 这一个入口，别在别处各算一遍「当前目录」。主目录给只读权限时命令照样跑不起来（执行命令与写文件同级，要求写权限），这一点与其它目录一致。
- **本地命令（`run_command` → `WorkspaceManager.exec`）一次也不跳访问策略**：命令能落到磁盘的任何地方，工作目录只是它的起点——按 cwd 放行等于给出一条绕过文件权限的路（工作区外写不了文件，改用命令写就够了）。所以 `ask` 逐条确认（横幅里连**命令原文**一起给出）、`auto` / `full` 放行、`deny` 直接拒绝，工作目录命中授权目录时另外要求写权限（只读目录不跑命令）；省略 `cwd` 时用项目的主目录（没标主目录就是第一个目录），项目没声明目录则直接拒绝。命令本身**不做字符串层面的拦截**：shell 里绕开关键字太容易，与其给人「拦住了」的错觉，不如让人看着命令原文决定。命令由主进程用系统 shell 执行（`spawn(..., { shell: true })`），带默认 60 秒、上限 10 分钟的超时（超时先 `taskkill /T /F` 收掉进程树再兜底 `kill`），两路输出各留 32KB（超出保留头尾、中间折成一行说明），非 UTF-8 输出（Windows 控制台常见 GBK）自动按 GBK 再解一次；退出码非 0 与超时都作为**调用失败**抛出（输出照旧带给模型，界面上这一格标红）。
- **访问申请是横幅不是弹窗，且各对话相互独立**（`components/Chat/ApprovalBanner.vue`，队列在 `stores/approval.ts`）：申请逐条确认，三个动作「拒绝 / 允许本次 / 始终允许」都是一步到位——弹窗会挡住消息与输入，横幅不打断这一轮；「始终允许」等于把策略切成「帮我批准」（主进程改策略，store 同步界面上的显示）。**申请带着发起它的 `conversationId` 上来**（工具调用一路传下来：chat store → `core/Toolbox.invoke` → 预加载 → 主进程 `WorkspaceManager` 的 workspace 方法末尾都带这个参数），界面只把申请交给那段对话：横幅摆在该对话的输入框上方，别的对话看不到、也答不了；`currentOf` / `pendingOf` 按会话标识精确匹配，一个对话的审批不会串到另一个，正在提交的状态也按申请记（`submittingId`），只让被点的那一组按钮转圈。不看着那段对话时，**侧边栏会话项上的琥珀色点**（`SidebarContent` / `SidebarConversationItem`）提示「有访问申请待确认」，点进去由横幅接手；等不到答复的申请由主进程按超时拒绝并推 `workspace:approval-closed`，界面据此把横幅与点一并撤掉（不留点不动的僵尸申请）。
- **本地命令（`run_command` → `WorkspaceManager.exec`）与文件操作同一套口径**：命令的**工作目录**落在哪个授权目录就按那个目录的权限判定（`read` / `write` / `command` 三档需求里，执行命令与写文件同级，要求写权限），省略 `cwd` 时用项目的主目录（没标主目录就是第一个目录），项目没声明目录则直接拒绝；越界时同样走 `workspacePolicy`，推给界面的申请里带上**命令原文**（横幅会把它单独列一行），人在看清命令后再决定。命令由主进程用系统 shell 执行（`spawn(..., { shell: true })`），带默认 60 秒、上限 10 分钟的超时（超时先 `taskkill /T /F` 收掉进程树再兜底 `kill`），两路输出各留 32KB（超出保留头尾、中间折成一行说明），非 UTF-8 输出（Windows 控制台常见 GBK）自动按 GBK 再解一次；退出码非 0 与超时都作为**调用失败**抛出（输出照旧带给模型，界面上这一格标红）。
- **输入框两个入口**（`components/Chat/ChatComposer.vue`）：`@` 引用工作区文件与已安装技能，候选浮层顶部还能经系统文件选择框引用**任意**本地文件 / 文件夹（选择时不做任何路径限制与权限判定，发送时读取内容或目录清单才按项目目录权限与工作区外策略校验）；`/` 执行内置指令（新建对话 / 切换项目 / 项目与工作区 / 技能管理 / 本地访问策略 / 上传文件 / 停止生成 / 清空当前对话 / 帮助）。
- **上下文用量圆环**（`components/Chat/ContextRing.vue` + `core/Tokens.ts`）：发送按钮左边一圈，画「下一轮请求大致要占多少 / 窗口多大」。窗口口径是**手工值优先**：项目配置里的 `projects.context_length`（`ProjectEditor.vue` 的「上下文大小」字段，圆环气泡里也能改，两者写的是同一处，0 = 不在项目里指定）> 服务端在模型清单里声明的 `contextLength`（`/bot/models` ← 网关 `/v1/models` 的 `context_length` ← 模型管理里的「上下文长度」）；两份都没有时圆环退化成问号、由用户填一个。用量是客户端按字符折算的**估算值**（`core/Tokens.ts`，客户端没有分词器），口径与 `run()` 组装报文一致（系统提示词 + 当前分支含 @ 引用正文 + 工具定义 + 待发内容），chat store 的 `contextUsage` 供页面取用。到 70% / 90% 圆环转黄 / 转红。
- **对话支持分支**：本地库 `messages.parent_id` 把消息连成消息树、`conversations.leaf_id` 记当前分支尾，页面只渲染「分支尾沿 parentId 回溯到根」的那条路径（`stores/chat.ts` 的 `visible` / `branchOf` / `switchBranch`）。编辑提问重新发送、重新生成回复都会新起一条分支（原分支的消息与调用链照旧保留），消息下方用「◀ n/m ▶」切换；模型上下文取当前分支路径，工具调用过程按「模型往返轮次」分段展示在回复气泡里（`components/Chat/ToolCallList.vue`）。
- **回复正文按 Markdown 渲染**（`components/Chat/ChatMarkdown.vue` + `core/Markdown.ts`）：marked 转换、DOMPurify 清洗后才上屏（模型输出不可信，渲染进程手里有 ipc 通道），链接统一补 `target="_blank"` 交给主进程的窗口打开策略；用户提问保持原文，工具调用参数与返回仍是等宽文本块。
- **正文里的知识库图片按当前授权实时签发地址**（`core/Markdown.ts` 的 `knowledgeImage` 选项 + `utils/KnowledgeImageUtil` + `api/agent/KnowledgeImageApi`）：知识库文档里的图片以稳定标识保存（形如 `![说明](kb:图片标识)`），展示时标识交给 marked 的图片渲染器换成地址，由 `/knowledgeImage/url` 批量换取带时效的原图地址——**每条正文各自解析、不做跨消息缓存**，授权变更后历史消息里的图片同步不可见；服务端只返回有权访问的图片，取不到地址（无权限 / 已删除 / 请求失败）与地址过期都回落本地默认图（`assets/images/knowledge-image-fallback.svg`，解析期间用 `knowledge-image-loading.svg` 占位）；图片点一下用 `el-image-viewer` 看大图，同一段正文里的图片按出现顺序左右切换。
- **滚动统一走 `el-scrollbar`**：内容区、侧边栏会话列表、消息区、电梯导航、@ 候选菜单、工具调用返回块、方法选择列表都用 Element Plus 的滚动条，不再各自写 `::-webkit-scrollbar`；消息区拿内部 `wrapElement` 作为滚动基准（自动贴底与电梯导航都用它）。
