# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`fs-bot` is an Electron desktop application built with Vue 3 and TypeScript. It was scaffolded from the `electron-vite` quick start template.

## Package Manager

This project uses **pnpm**. All scripts and install commands must use `pnpm`, not npm or yarn.

## Commands

| Command               | Description                         |
| --------------------- | ----------------------------------- |
| `pnpm install`        | Install dependencies                |
| `pnpm dev`            | Start development with HMR          |
| `pnpm start`          | Preview the built app               |
| `pnpm build`          | Typecheck and build for production  |
| `pnpm format`         | Format all files with Prettier      |
| `pnpm lint`           | Lint all files with ESLint (cached) |
| `pnpm typecheck`      | Run both node and web typechecks    |
| `pnpm typecheck:node` | Typecheck main + preload processes  |
| `pnpm typecheck:web`  | Typecheck renderer (Vue) process    |
| `pnpm build:win`      | Build Windows installer             |
| `pnpm build:mac`      | Build macOS app                     |
| `pnpm build:linux`    | Build Linux packages                |
| `pnpm build:unpack`   | Build unpacked (no installer)       |

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
- **User data (SQL)** — per-user encrypted SQLite at `userdata/<userId>/fs-bot.db` (SQLCipher + WAL, integrity hash in `_integrity`). `core/Db.ts` is the IPC bridge; `db/<Domain>Db.ts` wraps it per table (`ConversationDb`, `ProjectDb`, `TaskDb`) and returns `ApiUtil` result envelopes. 桥接层除增删改查外还给一个**关键词检索**（`Db.search` → `DatabaseManager.search`：表 + 列清单 + 关键词，可加等值 / 集合条件与 `orderBy` / `limit`；只用于「按内容找行」的查询，例如历史对话检索——几百上千条消息交给 SQLite 一条语句查完，比整表拉回渲染进程再过滤干净得多）。**关键词有两种口径**（`mode`）：`literal`（默认）整段按字面量匹配（`%` / `_` / `\` 转义，换行也是字面量），`segments` 按空白切成几段、各段按原顺序出现即算命中（`%段1%段2%`）——从对话里整段粘来的多行片段，库里的换行与缩进未必与它一致，字面量一条都命中不了时用 `segments` 兜底（见 `ConversationDb.search`：先字面量查一遍，空手且关键词能切成多段再查一遍）。`db/` is reserved for SQL — do not put file-backed accessors here.
- **User DB schema versions** — one SQL file pair per version, flat in `src/main/database/sql/`: `schema-vN.sql` is the complete schema used for new databases, `migrations-vN.sql` is the incremental upgrade script applied to existing ones. Register both in `schema.ts` (`USER_SCHEMAS`) and `migrations.ts` (`USER_MIGRATIONS`); `USER_SCHEMA_VERSION` is derived from the registry. `ensureUserSchema()` runs either path inside a single `SAVEPOINT`, refuses downgrades, and fails loudly on a missing script. A newer user DB triggers a native「客户端版本过低」prompt with both versions; confirming it exits the app. Conventions: `sql/README.md`; operation / upgrade / rollback runbook: `scripts/README.md` (`pnpm schema version|key|export`).
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

（项目编辑器里的「环境变量」不是这一套：那些变量用于本机命令，非密钥值还会随本机说明下发，见下文「项目环境变量」。）

| File               | When loaded  |
| ------------------ | ------------ |
| `.env.development` | `pnpm dev`   |
| `.env.production`  | `pnpm build` |

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
- **侧边栏可以拖拽调整宽度**（`views/index.vue` + `stores/sidebar.ts`）：侧边栏与内容区由 `el-splitter` 分隔，拖分隔条即可改宽度（限 200–480px），松手落到 `localStorage`（`fs_sidebar_width`），下次打开还是这个宽度；分隔条本身（hover 高亮主色）就是与内容区的分界线，侧边栏不再自己画右边框。宽度按像素存（不是百分比），窗口缩放不会把宽度按比例漂走。**`TitleBar` 的置顶按钮（钉住）意味着只留对话**（`stores/window.ts` 记这个状态，`components/Layout/TitleBar.vue` 切换）：窗口收成 400×600 的置顶窄条时，侧边栏与右侧文件区（`components/Chat/ConversationFiles.vue` + `stores/files.ts`）一并收起，对话头部那个文件区开关（`views/home/HomePage.vue`）也不再出现，取消置顶再回到钉住前的展开状态——窄条期间的收起是临时状态，不写 `fs_files_open`，免得被记成本机偏好。**右侧文件区展开 / 收起不挤占对话列**：`stores/files.ts` 把展开状态与宽度交给主进程（`window:files-panel`），窗口按文件区宽度等量加宽 / 收窄，拖分隔条也同步；靠近屏幕右缘时整窗左移，完全收起再还回原位（`main/index.ts` 的 `applyFilesPanelWidth`）。**macOS 下标题栏整条浮在窗口顶上**（红绿灯在左，`views/index.vue` 的 `.main-layout--mac`）：侧边栏在时由侧边栏自己让出上边距（`LayoutSidebar.vue`），侧边栏收起后（钉住就只留对话）改由内容区让（`main-right--mac-titlebar` 的 `padding-top`）——少了这一步，红绿灯会压住会话头部的项目名与标题。
- **Server-owned catalogs stay server-owned.** 应用（编排）、技能、知识库、工具方法、数据主题、知识图谱本体、模型清单都在服务端维护，桌面端不提供管理页面：只在项目里选择引用（见 `ProjectEditor.vue` 的「模型」与「工具」两段），本地只存「选中了哪些」。模型是「输入框 + 检索候选」（`el-autocomplete`，取值就是输入框文本，清单里没有的模型也能手填）；工具候选行与已添加的行都带一个帮助按钮，点开看服务端给出的描述（工具方法为方法描述，添加时随行保存）。**技能是其中唯一「服务端维护、本机安装」的一类**：编辑器「工具」段的类型下拉里选「本地技能」（下拉第一项），候选来自本机已安装清单（`main/skills.ts` 解压出来的那份），添加时只记 `kind: skill` + 技能标识 + 名称 + **推荐版本**——导出 / 分享 / 导入都随项目配置走，但版本只是个提示：安装不分版本、一个技能一份，运行时以技能目录里装的那一版为准，本机没装的行标「本机未安装」。手工放进技能目录的（没有 `meta.json`）没有服务端标识，列出来但不能引用。
- **项目配置可以导入 / 导出，也可以发布到服务端分享**，分享不单独占一个入口：项目管理页（`views/project/ProjectManagement.vue`）里分「我的项目 / 我的分享 / 浏览分享」三个页签，列表与表单仍是「表单替换列表、占满内容区」那套（项目表单与分享表单各自一个视图）。文件格式在 `utils/ProjectUtil.ts` 里定义：`{ type: 'fs-bot-project', version, exportedAt, share, project }`，`share` 只在来自分享时为对象；读写都由主进程弹原生对话框完成（`main/projects.ts`，IPC `project:export` / `project:import`），渲染进程只给文件内容或拿回原文。导出的入口是项目编辑器底部的「导出配置」（导出当前表单，未保存的改动也一并导出），导入的入口是页头的「导入项目」（重名自动加「（导入）」后缀，来自分享的文件会在提示里写明来源）。分享按「项目 + 版本」发布到服务端（`fs_agent_project_share` / `fs_agent_project_version`，见 `components/Project/ShareList.vue` 与 `ProjectShareForm.vue`）：编辑器底部的「分享」先保存再打开分享表单，「我的分享」卡片上的「分享新版本」直接把目标锁到那个分享项目，表单里选本地项目、填版本号与版本说明即可发布；自己的分享可以继续追加版本、导出任一版本，浏览分享里可以导出别人的配置或直接创建成本地项目（服务端只给已启用且授权命中的项目与版本）。**版本号收在卡片右上角的下拉里，用的是公共组件 `components/Version/VersionSelect.vue`**（技能管理页与分享卡片共用，一处改样式两边都受益）：版本数据源由调用方以 `loader` 注入——分享卡片按页问 `/bot/project/versions` 要（检索走服务端的 `name` 参数，不在前端过滤已取回的那一页），技能卡片一次取回该技能的全部启用版本（检索在 loader 里本地过滤）；面板里有检索框、当前版本打「✓」、`applied` 的那一版标「已安装」，选中哪一版由调用方决定用途（分享卡片据它导出配置 / 创建本地项目，技能卡片据它下载安装）。列表与面板都按页取（项目每页 30、面板每页 10，面板自己限高滚动——卡片里不套内嵌滚动条，页面本身就在滚），服务端列表行的版本数量与最新版本走轻量查询（逐项 COUNT + 只取最新一条，不把配置原文拖回来）；技能清单（`/skill/available`）同样带上了 `versionCount` 与 `latestVersion`，技能卡片不必再为显示一个版本号把每个技能的版本都请求一遍。本地项目用 `projects.share_id` / `share_name` / `share_version` 记住分享去向（卡片上回显「已分享 v1.0」，再次分享默认选中同一个分享项目）——这三个字段只是本地便签，服务端才是分享内容的权威存储。**分享内容包含环境变量（标了「密钥」的行除外，见下条）、长期记忆（`projects.memory`）与工作区目录路径**，发布前页面上有明确提示。
- **项目只有一条执行路径：模型。** 项目用 `model` + `contextLength` + `systemPrompt` + `memory` + `memoryEnabled` + `thinkEffort` + `tools` + `workspace` 描述自己，不绑定编排应用；对话由客户端经 `core/ModelChat` 调客户端专用的 `/bot/models`、`/bot/chat`（历史由客户端带上、会话与消息只落本地库；`systemPrompt`、`memoryEnabled` 为真时的 `memory` 与 `thinkEffort` 按会话所属项目随每轮请求下发，留空则不发、由模型与网关取默认值）。
- **长期记忆单独占项目配置的一格（`projects.memory` + `projects.memory_enabled`，项目编辑器的「启用记忆 / 记忆」区）**：**开关默认关闭**，关闭时记忆内容不下发、`buildTools` 也不把 `memory` 函数放进模型工具清单（工具清单缓存键带上这个开关，切回来不会拿到旧定义）；打开后才把记忆随每轮提示词下发，并允许模型通过 `memory` 工具读写。与系统提示词分开写，写入有两个来源——**编辑器里手工改**，以及**对话里模型用 `memory` 工具的 `write` / `replace` 动作改**（`write` 是整段写：`content` + `mode`（`append` 追加 / `replace` 整段替换），`content` 给空串配 `replace` 即**清空记忆**；`replace` 是只改一段：`find` → `content`，`content` 空串 = 删掉那一段；两者都只作用于当前对话所属的项目，对话没有所属项目就直接报错，写入走项目 store，页面上的编辑器打开就是最新内容）。**工具说明里明确要求：只有用户主动要求记住 / 保存 / 更新记忆时才调用 `write` / `replace`；用户没有主动要求时，即使对话里出现值得长期保留的偏好或事实，也不要自行归纳、补充或整理记忆。** **编辑口径（清空、唯一性、字面量匹配、行号）都收在 `utils/MemoryUtil.ts` 的纯函数里**（`applyWrite` / `applyReplace`，可单独跑），工具层只负责落库与回执——回执说清改了哪一行、几处、改完共几行几字，内容没变就不写库（免得白改一次「最后更新」）。下发时 `stores/chat.ts` 的 `memoryBlock` 只在开关打开时给它加上 `# 记忆` 标题，`systemPromptOf` 按「系统提示词 → 记忆 → 本机说明」合成同一条 system 消息（服务端不区分这三段）；项目里所有会话共用，改完保存后下一次提问生效（`ChatProfile.memory` / `memoryEnabled` 随 `chatProfile(project)` 一起走，编辑 / 重新生成那两条手搓 profile 的路径也各自带上），模型刚写进来的内容则由调用循环每轮从项目重取一次（`stores/chat.ts` 的 `run` 每轮刷新 `session.memory` 与 `session.memoryEnabled`）——所以工具写入从**下一轮**请求起就下发，本项目的其他会话也从各自下一轮起看得到。它和系统提示词一样**每轮都占上下文**（计入用量圆环），所以编辑器里写明「只写长期有用的事实，别抄整段对话」，工具说明里同样写死这条（写超 8000 字时调用结果里再提醒一次精简，见 `MEMORY_HINT_SIZE`）。导出 / 分享 / 导入都随项目配置走（`utils/ProjectUtil.ts` 的 `ProjectConfig` 有 `memory` 与 `memoryEnabled`，旧配置缺开关时按关闭处理），发布前的提示语里已并列「记忆」：记忆里写了私密内容，分享出去就会一并带出。**schema v12** 加 `projects.memory_enabled INTEGER DEFAULT 0`（v11 的 `memory` 列保留），老项目升级后默认关闭，需要长期记忆时在编辑器里打开开关。
- **系统提示词后面还追加一段「本机」说明**（`WorkspaceManager.describe` → IPC `workspace:prompt` → `stores/chat.ts` 的 `systemPromptOf`），**只列事实、不写解释**：`# 环境信息` 下是操作系统与架构、命令用的 shell，`## 工作区` 下按行列出目录与权限（主目录另标），零目录时换成一句「未声明工作区目录：文件操作必须给绝对路径，执行命令会被拒绝」，`## 环境变量` 也用每项一行的列表下发非密钥值。这段随每轮请求下发、占的是上下文，所以加内容前先掂量值不值（相对路径与命令默认落在主目录这类口径已经在工具说明里写了，不重复）。它每轮重取（目录、主目录、环境变量改了当轮生效），也计入上下文用量圆环，服务端把它与项目提示词合成同一条 system 消息。工具按类型添加（工具方法 / 知识库 / 数据主题 / 知识图谱 / 编排应用，与服务端编排里大语言模型节点的工具种类一致），凭据与调用都在服务端；编排应用只是其中一种工具类型。

- **项目环境变量**（`projects.env_vars`，项目编辑器的「环境变量」区）：执行本机命令（`run_command` → `WorkspaceManager.exec`）时合成子进程环境，规则在 `main/project-env.ts`——按名覆盖进程环境变量（Windows 上不区分大小写，同名只留一条）、`PATH` **前置**（把目标 JDK 的 bin 目录写进去就能切换 `java`，不必把系统 PATH 抄一遍）、值原样使用不做变量展开；变量名限「字母或下划线开头，只含字母 / 数字 / 下划线」，不合规的行解析时丢掉（`parseProjectEnvVars`），编辑器里对这类名字当场给一条 `section-warning`（`invalidEnvNames`），不让它保存完才发现读不到。主进程按项目读这一列（`workspace.setProjectEnvResolver`，读不到就给空、不挡命令执行），所以「同一台机器装了 JDK 8 与 JDK 17、按项目切换」由项目配置决定，不动系统环境变量。**两种变量两种口径**：不标「密钥」的随分享上传，并在本机说明的 `## 环境变量` 里按工作区同样的每项一行格式下发 `KEY=VALUE`；标了「密钥」的只留本机——发布前 `utils/ProjectUtil.ts` 的 `publishConfig` 把它整行摘掉（服务端明文存储），本机说明只列变量名并标明「值不下发」，命令输出里的值由 `redactSecretValues` 换成 `***`（`runCommand` 收尾时替换 stdout / stderr）。非密钥值因此不必再执行回显命令才能看到，密钥值也不会进入提示词；脱敏是按值精确替换：拦得住回显与日志，拦不住刻意编码后的外传（base64 / certutil 之类），密钥该交给脚本用；值只有几个字符时会连普通文本一起命中，这类值别标成密钥。本机导出的配置文件（`projectFile`）是这台机器的备份，仍带全部环境变量（编辑器提示语就是这套口径的现场说明）。**值那一格右侧两个后缀按钮**分别选本机文件 / 目录（`workspace:pick` 由 `pick()` 变成 `pick(kind, title)`，kind 取 file / directory、一次只选一个；工作区那两处仍走它、标题照旧），选中的路径整体填进这一格。掩码只认「密钥」那一行（`show-password` 绑 `item.secret`：早先写死，el-input 一开它就忽略 `type`，普通值也被点成圆点）。

### 对话（一轮 = 模型 → 工具 → 模型）

- **调用循环跑在客户端**（`stores/chat.ts`）：一轮对话里模型可以反复请求工具，客户端每次把工具结果接回上下文再让模型继续，直到模型给出最终回复。轮次上限不是硬终止：跑满一批 `MAX_ROUNDS` 轮仍在请求工具就自动接续下一批，接续点随请求补一条 `continuePrompt` 提示词（讲清已跑多少轮、还剩多少轮，点名无效轮转的风险，同时说明没有异常就照常推进、别为省轮数提前收尾；上下文不清空，且**只保留最新一条**，链里不会堆起多条催促）；接续 `MAX_ROUNDS_CONTINUE` 次后再做一次**不带工具的收尾调用**——工具往返 = `MAX_ROUNDS × (1 + MAX_ROUNDS_CONTINUE)`，收尾是额外加的一轮、**不占用批次数**（每一批都是完整的 `MAX_ROUNDS` 轮），让模型按已完成的进度给出结论，这一轮记 `done` 并给一条非错误提示（`session.notice`，可回复「继续」接着做），不再像过去那样判失败。会话记录里只留模型自己的产出，提示词不落库、不进后续轮次的上下文。会话历史由客户端维护，因此每轮的完整调用链（`assistant` 的 `tool_calls` + 逐条 `tool` 返回）都随下一次请求一起带上，模型据此续推；调用链随消息落在 `messages.reference`（JSON），历史会话打开后按同一形状还原。
- **工具由 `core/Toolbox.ts` 归一**：本地工具一共五个——`file_op`（文件与目录）、`run_command`（本机命令）、`schedule_task`（定时任务）、`memory`（历史对话 + 长期记忆）与 `local_skill`（本地技能）；文件与命令由主进程执行，定时任务、历史对话与长期记忆走本地库（渲染进程），本地技能的清单与文件由主进程读技能目录。项目工具（工具方法 / 知识库 / 数据主题 / 知识图谱本体 / 编排应用）的定义与执行都在服务端（`/bot/tools` 取定义、`/bot/tool` 执行一次），客户端只做函数名分派与调用链拼装。**记忆与技能按项目开关决定下不下发**：`memoryEnabled` 为假时 `memory` 整个 function 不出现（即使模型硬调也由执行层拒绝），`kind: skill` 的引用从 `/bot/tools` 的入参里摘出来（服务端不认这一类），项目一个技能都没启用时 `local_skill` 整个不下发——没启用但本机装着的不给这个项目用。
- **同一实体的多个操作合成一个工具、用 `action` 参数区分**（`file_op` / `schedule_task` / `memory` / `local_skill`；`run_command` 单列，因为它的作用范围不限于工作目录、每次都要过访问策略，与文件操作不是一套口径）：每轮请求都要带上整份工具定义，一个实体拆成五个 function 光骨架就白占几百 token（本地工具从 19 个 function 收到 5 个）。代价是 JSON Schema 表达不了「哪个 action 要哪些参数」，所以 schema 里只把 `action` 标成必填（带 `enum`），条件必填与取值校验放到执行期——`actionOf` 先核一遍 action（不在清单里就把可选项列出来），各分支再校验自己缺什么（`file_op` 用局部 `need`，如 `read` 要 `path`、`move` 要 `from` / `to`）。**「空」的表达只有一种：空串**——`write` 的 `content`、`replace` 的 `replace` / `content` 给空串都是有意义的行为（清空文件 / 删掉那一段），`undefined` 与 `null` 一律按「没给」处理并报错指路（`utils/ArgUtil.ts` 的 `missing` / `textArg`，文件与记忆两侧共用）：把 JSON 的 `null` 当空串会静默做出破坏性动作，当文本又会写进一个字面的 "null"，两种都不该发生。错误信息照旧作为调用失败回到模型，模型据此补齐后重试。**工具名取产品语言，泛称的歧义由描述第一句兜住**：这组能力对外就叫「记忆」，所以工具名是 `memory`；两类「记忆」都收在这一个工具里——`list` / `search` / `read` 翻本机历史对话（只读），`write` / `replace` 改当前项目的长期记忆（`projects.memory`），描述第一句把两件事都点出来，模型不会再把 `write` 当成不存在的动作去试。
  - `file_op` 的 `action` 取 `read` / `write` / `replace` / `list` / `mkdir` / `move` / `delete` / `search` / `upload`：原来那批文件与目录工具都在，`replace` 是**文本替换**、`upload` 是原来的 `upload_file`（上传到文件服务拿 `id`，服务端工具方法按 `id` 引用本机文件）；路径口径、主目录优先、越界走访问策略这些都没变。**检索与替换共用一套匹配口径**（`WorkspaceManager` 的 `keywordHits`）：先按字面量整段找（关键词里的换行对应文件里的换行，`\r\n` 与 `\n` 在内部先统一），一条都没命中、而关键词又能按空白切成多段时，再按「各段按原顺序出现、段间任意空白」找一遍——从对话里整段粘来的多行片段（缩进、`\r\n` 与 `\n` 的差别）也能命中；一律按字面量，正则元字符不当语法用。`search`（`WorkspaceManager.search`）命中给出行号（跨行给 `12-15`）与**覆盖到的整行**（跨行命中给整段，同一行只报一条，正好能拿去当 `replace` 的 `find`）；单文件上限 1MB、命中上限 200、含 NUL 字节的二进制跳过、中文与英文都按字面端配（英文区分大小写）。`replace`（`WorkspaceManager.replace`）把 `find` 换成新文、文件其余内容原样保留：`start_line` / `end_line` 限定行范围（1 起、闭区间，省略即整个文件），范围内**默认要求恰好一处命中**，0 处或多处都失败并把位置回报（免得改错地方），给 `all=true` 才替换范围内的全部命中；行尾（LF / CRLF）与 BOM 原样保留、替换内容里的换行也按原行尾写出，只处理 UTF-8 文本（二进制与非 UTF-8 一律拒绝、不静默转码），单文件上限 8MB；回执用 `\n` / `\t` 把替掉的原文与新文显示出来（按分段口径命中时两段之间的空白也在替换范围内，替换内容要自带缩进）。两条都跑在主进程（IPC `workspace:search` / `workspace:replace`），权限与 `write` 同一套。
  - 定时任务 `schedule_task`：`action` 取 `list` / `create` / `update` / `delete` / `run`。走 `stores/schedule.ts`，与「定时任务」页面共用同一份数据（模型改完页面立刻刷新，两边读的都是 `tasks` 表）；cron 落库前按页面那套口径校验（5 段：分 时 日 月 周），没给 `project_id` 时默认当前对话所属的项目，`run` 真的发起一轮后台对话（等于页面上的「运行一次」，会花掉一次模型调用）。
  - 记忆 `memory`：`action=list` 按时间倒序列出范围内的会话（标题、时间与会话 id，先看有哪些对话）；`action=search` 按关键词查本地库的**会话标题与消息正文**，命中给会话 id、标题、角色、时间、消息在会话里的**条号**（`第 n/共 m 条`）与关键词附近的片段——条号就是 `read` 的 `offset + 1`，模型据此一步跳到命中处附近，不必从头翻；关键词先按字面量整段匹配，一条都没命中且关键词能按空白切成多段时再按「各段按原顺序出现」查一遍（`mode=segments`，整段粘来的多行片段靠它兜底）；片段与关键词都先折叠空白再取位置，多行关键词也能围着命中处取片段；`action=read` 按会话 id 读消息正文（角色 + 时间 + 工具调用次数，按 `offset` 分段落读，会话里若留有编辑 / 重新生成产生的**旧分支消息**会在开头点明，免得模型把分支当成一条线）。
    `action=write` **只在用户主动要求记住 / 保存 / 更新记忆时使用**，把长期有用的事写进当前项目的 `projects.memory`：`content` + `mode`（`append` 追加 / `replace` 整段替换），`content` 给空串配 `replace` 就是**清空记忆**（`append` 配空串报错，并指出该用 `replace` 清空）；用户没有主动要求时不要自行写入或整理记忆。`action=replace` 只改记忆里的一段：`find` → `content`（`content` 空串即删掉那一段），按字面量匹配、默认要求恰好一处命中（命中多处才给 `all`），记忆为空或找不到就报错、不猜着改。两种写法的落点都在 `utils/MemoryUtil.ts` 里算（纯函数，见上文「长期记忆」那条）。
    `list` 与 `search` **默认只覆盖当前对话所属的项目**（`scope: all` 才跨项目；会话本身没有所属项目时退回全部项目并在结果里写明范围）。检索由主进程一条 SQL 查完（`db:search` → `DatabaseManager.search`：`columns` 逐个 LIKE、可加等值（`where`）与集合（`in`）条件、`orderBy` / `limit`；关键词与取值都走占位符，`%` / `_` / `\` 按字面量转义），不把整表拉回渲染进程过滤；跨项目检索时把会话清单按上限截断，兜住 IN 列表的长度；命中条号按会话逐个算（同一个会话只取一次消息清单）。
  - 本地技能 `local_skill`：`action` 取 `list` / `files` / `read`，读本地 `skills` 目录（服务端技能包解压出来的那份，见 `main/skills.ts`）里**本项目启用的**技能：`list` 只列启用清单（本机没装的也标出来，并带上项目里的推荐版本），`files` / `read` 只认这些名字，别的名字直接拒绝并指路到项目编辑器。`list` 每个技能带一行**摘要**（`SKILL.md` frontmatter 的 `description`，支持 `|` / `>` 块写法；没有就取第一段正文并跳过 frontmatter），模型看这行就能挑技能、不必逐个读说明；`read` 省略 `path` 就是读 `SKILL.md` —— 技能的主入口，模型照它做事，再按 `files` 给的相对路径读脚本与参考文档；相对路径由主进程限制在技能目录内（技能包内容按不可信输入处理），非文本文件（前 8KB 里有 NUL）直接拒绝、单文件超 256KB 按上限截断。技能里的脚本要跑仍走 `run_command`（因此照样过一遍本地访问策略，`skills` 目录通常不在项目工作区里，会按「工作区外」申请），`@skill:名称` 那条路（把说明正文引用进上下文）不变——它是用户自己挑的技能，与项目启用与否无关。
- **本机文件交给服务端工具方法要先上传（`file_op` 的 `action=upload` 与 `/upload`）**：服务端读不到客户端的磁盘，本机路径传给 `multimodalAnalyze` 这类工具方法会被拒绝（服务端只认文件内容、可访问地址或文件服务的 `id`）。本地文件走「主进程读内容 → multipart 直传 `/bot/upload` → 拿回 `{ id, name, type, suffix, size }` → 模型按 `id` 引用」这条路：模型可以自己调本地工具 `file_op`（`action=upload`，它能从用户那句话里拿到路径），用户也可以在输入框里用 `/upload` 选文件、把文件标识等信息写进消息；两条路共用同一个上传通道与同一份本地访问策略（读取没被放行就不会上传）。
- **工具方法可以把参数固定下来**（`ProjectEditor.vue` 的「参数」 → `db/ProjectDb.ts` 的 `ProjectTool.args`）：某个参数切成「固定」（对象参数按字段逐个配）后，取值随项目落库，跑起来时由服务端补齐 —— `/bot/tools`、`/bot/tool` 把项目里的工具引用原样交给工具实现（`BotService.item` 透传 `args`），因此定义里会自动摘掉这些参数（模型不必再生成，也不会与配置打架）、调用时按取值覆盖（对象参数只覆盖同名的那几个字段，其余仍用模型给的，取值按文本下发）。这套绑定与编排里大语言模型节点的执行变量是同一个实现（`AgenticTool.definition` + `MethodTool.invoke`）。面板渲染期按需补出「自动 / 空值」的默认绑定，保存前统一整理成「只留固定项」（留空视为不固定）。
- **工作区权限是主进程的事**：项目声明的工作区目录按各自的读 / 写权限放行；**工作区之外**的路径按访问策略处理（`workspacePolicy`，`userData/config.toml` + 系统设置页面）：`ask` 请求批准（主进程推 `workspace:approval-request` 给界面，答复走 `workspace:approval:response`）、`auto` 帮我批准、`deny` 自动拒绝、`full` 完全访问权限。策略的判定与执行都在主进程，渲染进程改不了结论；被拒绝时失败原因作为工具返回交给模型，本轮对话不中断。**命令不适用「落在授权目录内就放行」这条**（见下一条）。
- **清单里可以标一个主目录**（`workspace[].primary`，项目编辑器「工作区」一节点「设为主目录」，至多一个）：命令省略 `cwd` 时在它里面跑，相对路径按「主目录优先、其余按声明顺序」解析——先取已经存在的那一份，都没有就落到主目录，新建文件因此默认落在主目录里。没标主目录的旧配置与分享来的配置一律按第一个目录，行为与从前一致。取用只走 `WorkspaceManager.primaryOf` 这一个入口，别在别处各算一遍「当前目录」。主目录给只读权限时命令照样跑不起来（执行命令与写文件同级，要求写权限），这一点与其它目录一致。
- **本地命令（`run_command` → `WorkspaceManager.exec`）一次也不跳访问策略**：命令能落到磁盘的任何地方，工作目录只是它的起点——按 cwd 放行等于给出一条绕过文件权限的路（工作区外写不了文件，改用命令写就够了）。所以 `ask` 逐条确认（横幅里连**命令原文**一起给出）、`auto` / `full` 放行、`deny` 直接拒绝，工作目录命中授权目录时另外要求写权限（只读目录不跑命令）；省略 `cwd` 时用项目的主目录（没标主目录就是第一个目录），项目没声明目录则直接拒绝。命令本身**不做字符串层面的拦截**：shell 里绕开关键字太容易，与其给人「拦住了」的错觉，不如让人看着命令原文决定。命令由主进程用系统 shell 执行（`spawn(..., { shell: true })`），**标准输入接空设备（`stdio: ['ignore', 'pipe', 'pipe']`）而不是 spawn 默认的管道**——默认管道父进程既不写也不关，命令只要去读 stdin（`findstr`、`sort`、`more`、`set /p`、要凭据的 CLI……）就会一直等着、期间一个字都不输出，看着就是「命令卡住」，最后拖到超时才被收掉；接空设备后这类命令立刻读到 EOF（要么正常结束、要么自己报错），与「无人值守跑一条命令」的口径一致。带默认 60 秒、上限 10 分钟的超时（超时先 `taskkill /T /F` 收掉进程树再兜底 `kill`），整个调用挂在 `close` 上、进程退出后再补一道 500ms 宽限（`FINISH_GRACE`，覆盖「命令起过常驻子进程、输出管道被它们握着」导致 `close` 迟到的情况），两路输出各留 32KB（超出保留头尾、中间折成一行说明），非 UTF-8 输出（Windows 控制台常见 GBK）自动按 GBK 再解一次；退出码非 0 与超时都作为**调用失败**抛出（输出照旧带给模型，界面上这一格标红）。
- **访问申请是横幅不是弹窗，且各对话相互独立**（`components/Chat/ApprovalBanner.vue`，队列在 `stores/approval.ts`）：申请逐条确认，三个动作「拒绝 / 允许本次 / 始终允许」都是一步到位——弹窗会挡住消息与输入，横幅不打断这一轮；「始终允许」等于把策略切成「帮我批准」（主进程改策略，store 同步界面上的显示）。**申请带着发起它的 `conversationId` 上来**（工具调用一路传下来：chat store → `core/Toolbox.invoke` → 预加载 → 主进程 `WorkspaceManager` 的 workspace 方法末尾都带这个参数），界面只把申请交给那段对话：横幅摆在该对话的输入框上方，别的对话看不到、也答不了；`currentOf` / `pendingOf` 按会话标识精确匹配，一个对话的审批不会串到另一个，正在提交的状态也按申请记（`submittingId`），只让被点的那一组按钮转圈。不看着那段对话时，**侧边栏会话项上的琥珀色点**（`SidebarContent` / `SidebarConversationItem`）提示「有访问申请待确认」，点进去由横幅接手；等不到答复的申请由主进程按超时拒绝并推 `workspace:approval-closed`，界面据此把横幅与点一并撤掉（不留点不动的僵尸申请）。
- **本地命令（`run_command` → `WorkspaceManager.exec`）与文件操作同一套口径**：命令的**工作目录**落在哪个授权目录就按那个目录的权限判定（`read` / `write` / `command` 三档需求里，执行命令与写文件同级，要求写权限），省略 `cwd` 时用项目的主目录（没标主目录就是第一个目录），项目没声明目录则直接拒绝；越界时同样走 `workspacePolicy`，推给界面的申请里带上**命令原文**（横幅会把它单独列一行），人在看清命令后再决定。命令由主进程用系统 shell 执行（`spawn(..., { shell: true })`），**标准输入接空设备（`stdio: ['ignore', 'pipe', 'pipe']`）而不是 spawn 默认的管道**——默认管道父进程既不写也不关，命令只要去读 stdin（`findstr`、`sort`、`more`、`set /p`、要凭据的 CLI……）就会一直等着、期间一个字都不输出，看着就是「命令卡住」，最后拖到超时才被收掉；接空设备后这类命令立刻读到 EOF（要么正常结束、要么自己报错），与「无人值守跑一条命令」的口径一致。带默认 60 秒、上限 10 分钟的超时（超时先 `taskkill /T /F` 收掉进程树再兜底 `kill`），整个调用挂在 `close` 上、进程退出后再补一道 500ms 宽限（`FINISH_GRACE`，覆盖「命令起过常驻子进程、输出管道被它们握着」导致 `close` 迟到的情况），两路输出各留 32KB（超出保留头尾、中间折成一行说明），非 UTF-8 输出（Windows 控制台常见 GBK）自动按 GBK 再解一次；退出码非 0 与超时都作为**调用失败**抛出（输出照旧带给模型，界面上这一格标红）。
- **输入框三个入口**（`components/Chat/ChatComposer.vue`）：`@` 引用工作区文件与已安装技能，候选浮层顶部还能经系统文件选择框引用**任意**本地文件 / 文件夹（选择时不做任何路径限制与权限判定，发送时读取内容或目录清单才按项目目录权限与工作区外策略校验）；**文件 / 文件夹直接拖进对话区**同样引用——**拖放区是中间那一列**（消息区 + 输入区，事件接在 `views/home/HomePage.vue` 的 `.conversation-column` 上；右侧文件区不算，那里的整行另有「拖出去」的用法），拖进来时整列盖一层虚线提示，松手即按输入框的光标位置插入 `@路径 `：页面只管「文件路径 → 输入框」，落点与插入格式仍由输入框那一处 `addPaths` 说了算（生成中不接，提示层也不亮）；`/` 执行内置指令（新建对话 / 切换项目 / 项目与工作区 / 技能管理 / 本地访问策略 / 上传文件 / 停止生成 / 清空当前对话 / 帮助）。拖入的路径由 preload 用 `webUtils.getPathForFile` 取出（**Electron 32 起 `File.path` 已移除**，File 对象不过 contextBridge，见 preload 的 `filePaths`），窗口别处的文件拖动由 `main.ts` 拦掉——不拦的话窗口会直接跳到 `file://` 打开那个文件，界面就此丢掉。
- **工作区里的文件能拖出窗口**（右侧文件区的两页签——工作区目录树与交付文件的每一行、工具调用卡片上「涉及的文件」每一张，整行 / 整卡可拖，见 `utils/FileActionUtil.dragOut`）：浏览器那套拖放给不出本机路径，原生拖放只能由主进程用 `webContents.startDrag` 发起——界面在 `dragstart` 里先 `preventDefault` 拦掉浏览器的默认拖放，再把路径 `send` 给 `workspace:drag`（主进程在同一时机接手，所以这条用 `on` 而不是 `handle`；拖拽影像是缩到 32px 的应用图标，见 `main/index.ts` 的 `dragIcon`）。拖到资源管理器 / 别的应用就是把这一份交给它，**拖回对话列（消息区 / 输入区）就是一条 `@` 引用**（两条路共用同一个 `addPaths`）；路径口径与 `open` / `reveal` 一致（用户自己拖出来的动作，只做路径归一与存在性判断，不套工作区外策略，可执行文件也照拖——复制出去不等于运行），服务端交付的外部地址那张仍是链接、按浏览器的默认拖放拖。
- **上下文用量圆环**（`components/Chat/ContextRing.vue` + `core/Tokens.ts`）：发送按钮左边一圈，画「下一轮请求大致要占多少 / 窗口多大」。窗口口径是**手工值优先**：项目配置里的 `projects.context_length`（`ProjectEditor.vue` 的「上下文大小」字段，圆环气泡里也能改，两者写的是同一处，0 = 不在项目里指定）> 服务端在模型清单里声明的 `contextLength`（`/bot/models` ← 网关 `/v1/models` 的 `context_length` ← 模型管理里的「上下文长度」）；两份都没有时圆环退化成问号、由用户填一个。用量是客户端按字符折算的**估算值**（`core/Tokens.ts`，客户端没有分词器），口径与 `run()` 组装报文一致（系统提示词 + 当前分支含 @ 引用正文 + 工具定义 + 待发内容），chat store 的 `contextUsage` 供页面取用。到 70% / 90% 圆环转黄 / 转红。
- **对话支持分支**：本地库 `messages.parent_id` 把消息连成消息树、`conversations.leaf_id` 记当前分支尾，页面只渲染「分支尾沿 parentId 回溯到根」的那条路径（`stores/chat.ts` 的 `visible` / `branchOf` / `switchBranch`）。编辑提问重新发送、重新生成回复都会新起一条分支（原分支的消息与调用链照旧保留），消息下方用「◀ n/m ▶」切换；模型上下文取当前分支路径，工具调用过程按「模型往返轮次」分段展示在回复气泡里（`components/Chat/ToolCallList.vue`）。
- **删除会话 / 清空消息会先作废正在生成的那一轮**：生成挂在 chat store 上，删除只删库挡不住后台的模型与工具，因此 `chat` store 的 `discard` 会先标记 `stopping`、断开模型流并让主进程取消该会话的本地命令与待审批申请，再给这一轮记下「删除 / 清空」的收口方式。`persist` 在每个写库步骤前后都检查这个标记，删除 / 清空是最终状态：已开始的 `appendMessage` 也会在写完后被再清一遍，不会留下无主消息或清空后又冒出来的回复。侧边栏与历史页删除前都走 `utils/ConversationUtil` 确认；生成中确认按钮写「停止并删除」。`/clear` 生成中调用会停止本轮后清空，并提示已停止。
- **生成中仍可输入，发送自动排队**：输入框不再随 `running` 禁用；输入为空时右下角显示停止按钮，有内容时同一位置换成灰色发送按钮，点击即进入该会话的待发送队列。生成中提交不弹窗、不打断当前轮次，直接排队。`chat` store 在轮次循环的边界通过 `onQueuedPending` 检查队列：上一轮工具已跑完、或模型刚返回还要调工具时，当前回复提前收尾，不再等整段最终输出；`onRunSettled` 随后把当时队列里的所有消息按提交顺序合成一条（正文空行分段、`@` 引用去重）一次发出，不再逐条等待。队列按会话存在 `conversation` store 里，输入区上方用可关闭的标签展示，删除会话或 `/clear` 会一并清掉。输入框里尚未发送的文字与 `@` 引用也按会话保存在 `composerDrafts`（本次运行内），切换会话、开新会话或离开对话页再回来时原样恢复。
- **回复正文按 Markdown 渲染**（`components/Chat/ChatMarkdown.vue` + `core/Markdown.ts`）：marked 转换、DOMPurify 清洗后才上屏（模型输出不可信，渲染进程手里有 ipc 通道），链接统一补 `target="_blank"` 交给主进程的窗口打开策略；用户提问保持原文，工具调用参数与返回仍是等宽文本块。
- **正文里的知识库图片按当前授权实时签发地址**（`core/Markdown.ts` 的 `knowledgeImage` 选项 + `utils/KnowledgeImageUtil` + `api/agent/KnowledgeImageApi`）：知识库文档里的图片以稳定标识保存（形如 `![说明](kb:图片标识)`），展示时标识交给 marked 的图片渲染器换成地址，由 `/knowledgeImage/url` 批量换取带时效的原图地址——**每条正文各自解析、不做跨消息缓存**，授权变更后历史消息里的图片同步不可见；服务端只返回有权访问的图片，取不到地址（无权限 / 已删除 / 请求失败）与地址过期都回落本地默认图（`assets/images/knowledge-image-fallback.svg`，解析期间用 `knowledge-image-loading.svg` 占位）；图片点一下用 `el-image-viewer` 看大图，同一段正文里的图片按出现顺序左右切换。
- **正文里的本机图片只认 Markdown 图片语法**（`core/Markdown.ts` 的 `localImage` 选项 + `utils/LocalImageUtil` + `main/images.ts`）：模型写 `![说明](local:相对路径)` 时（也兼容 `file:///...`、盘符绝对路径与带图片后缀的相对路径），marked 的图片渲染器把本机路径交给 `workspace:image`，由主进程按项目权限读取并签发 `chat-image://<随机标识>` 地址——地址里不带路径，协议处理时只按内存映射回文件；只开放 png/jpg/jpeg/gif/webp/bmp/avif、单张 ≤ 20MB。**正文里的纯路径不会被自动转成图片，工具调用的交付路径也不会自动补图**；只有 Markdown 图片节点进入这条链路，解析期间占位、失败回落默认图，点击仍走 `el-image-viewer`。`WorkspaceManager.describe` 只附一句最小约定（Markdown 图片语法与一个例子），不展开格式细节，避免影响模型输出。
- **本机图片的路径与编码口径**：路径按运行平台解析（POSIX 绝对路径在 Linux / macOS 可用；Windows 上 `/...` 按当前盘符解析）；标准 `data:image/...;base64,...` 图片由现有 CSP 与 DOMPurify 的 `img` 数据 URI 规则直接放行，裸 base64 字符串与 `base64:` 前缀不认。
- **滚动统一走 `el-scrollbar`**：内容区、侧边栏会话列表、消息区、电梯导航、@ 候选菜单、工具调用返回块、方法选择列表都用 Element Plus 的滚动条，不再各自写 `::-webkit-scrollbar`；消息区拿内部 `wrapElement` 作为滚动基准（自动贴底与电梯导航都用它）。
