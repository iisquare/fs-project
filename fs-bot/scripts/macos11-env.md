# macOS 11 兼容开发环境

`macos11-env.mjs` 用于在低版本 macOS 上使用一套隔离的旧工具链开发、运行和打包，同时保持仓库主线的依赖和构建配置始终为最新版本。它不是正式运行脚本，也不会参与应用启动。

## 适用范围

- 支持 **macOS 11 Big Sur 及以上**。
- 不支持 macOS 10.15 Catalina 及更低版本。Electron 33 起已不支持 Catalina，本项目低版本模式使用的 Electron 37 仍支持 Big Sur，但 Electron 38 起要求 macOS 12。
- 支持 Intel `x64` 和 Apple Silicon `arm64`。构建架构必须与当前 Node.js 架构一致，不做交叉编译。

隔离环境只覆盖会阻止旧系统运行的关键版本：

| 组件     | 主线     | macOS 11 隔离环境 | 原因                                   |
| -------- | -------- | ----------------- | -------------------------------------- |
| Electron | 跟随主线 | `37.10.3`         | Electron 38 起不再支持 macOS 11        |
| Vite     | 跟随主线 | `~7.2.7`          | 已验证可与 Electron 37 和 Big Sur 配合 |
| esbuild  | 跟随主线 | `0.25.12`         | 新版本预编译二进制不再支持 macOS 11    |

其它依赖仍从当前主线的 `package.json` 读取。每次执行脚本时，脚本还会读取主线的 `pnpm-lock.yaml`；主线更新后会重新准备隔离环境。

## 前置条件

首次使用前安装：

1. Node.js 22。脚本会尝试通过已有的 nvm、fnm 或 Homebrew Node 22 重启自身；找不到时退出并给出提示。
2. pnpm 10 或更高版本。缺失时脚本会通过 Corepack 激活 `11.4.0`。
3. 支持 Big Sur 的 Xcode Command Line Tools：

   ```bash
   xcode-select --install
   ```

4. Python 3 仅在原生模块需要从源码编译时使用。脚本不会检测或设置 Python 环境变量。

## 快速使用

在项目根目录执行：

```bash
# 首次安装隔离依赖
node scripts/macos11-env.mjs install

# 开发运行
node scripts/macos11-env.mjs dev

# 预览已有的生产构建
node scripts/macos11-env.mjs start

# 类型检查、构建并打包 DMG + ZIP
node scripts/macos11-env.mjs build

# 打开使用隔离依赖的临时 shell
node scripts/macos11-env.mjs shell
```

`dev` 支持继续传递 electron-vite 参数，例如：

```bash
node scripts/macos11-env.mjs dev --debug
```

`start` 对应主线的 `electron-vite preview`，只运行已有的 `out/` 生产构建，不会自动重新构建或生成安装包。第一次执行前先运行 `build`；如果 `out/main/index.js` 不存在，脚本会提示先构建。

## 主线保护机制

脚本不会修改以下文件：

- `package.json`
- `pnpm-lock.yaml`
- `pnpm-workspace.yaml`
- `scripts/postinstall.mjs`
- `electron-builder.yml`

兼容依赖生成在主目录缓存中：

```text
~/Library/Caches/fs-bot/macos11/
```

执行 `dev`、`build` 或 `shell` 时，脚本会把主线 `node_modules` 临时移到：

```text
dist/.node_modules-mainline-macos11/
```

然后把项目根目录的 `node_modules` 临时链接到隔离环境。命令正常结束、按 `Ctrl+C` 或收到常规终止信号时，脚本会恢复主线 `node_modules`。

注意：

- 低版本环境运行期间，不要再从另一个终端执行 `pnpm install`，也不要把 `pnpm dev` 当作兼容环境的启动命令。脚本用文件锁阻止同时启动多个低版本环境。
- 隔离 Shell 中请直接使用 `electron-vite dev` 或 `electron-vite build`。退出该 Shell 后再执行 `macos11-env.mjs build`，不要在活动环境里重新跑 `pnpm install`。
- 主线版本和低版本环境共享同一份源码、配置入口和开发产物，但依赖目录会在命令执行期间切换。
- 脚本保证主线配置不被覆盖，但无法自动解决未来源码使用 Electron 38+ 新 API 的情况。`build` 会先执行类型检查，若主线代码与 Electron 37 不再兼容，需要单独增加兼容代码或停止支持 macOS 11。

## 输出与架构

`build` 会先执行与主线一致的 Node/Vue 类型检查，再构建应用并输出：

```text
dist/macos11/
├── fs-bot-<version>-mac11-x64.dmg
└── fs-bot-<version>-mac11-x64.zip
```

Apple Silicon 上默认输出 `arm64` 包。需要指定架构时：

```bash
FS_BOT_MAC11_ARCH=x64 node scripts/macos11-env.mjs build
```

指定架构必须与运行脚本的 Node.js 架构相同。在 Apple Silicon 上构建 x64 包时，需要使用 x64 Node.js 环境；不要混用 arm64 Node 和 x64 原生模块。

## 可选环境变量

| 变量                               | 默认值             | 说明                                 |
| ---------------------------------- | ------------------ | ------------------------------------ |
| `FS_BOT_NODE_VERSION`              | `22`               | 使用的 Node.js 大版本                |
| `FS_BOT_PNPM_VERSION`              | `11.4.0`           | 缺少 pnpm 时通过 Corepack 激活的版本 |
| `FS_BOT_MAC11_ARCH`                | 当前 Node 架构     | 构建架构，只允许 `x64` 或 `arm64`    |
| `ELECTRON_MIRROR`                  | 国内镜像           | Electron 二进制下载地址              |
| `ELECTRON_BUILDER_BINARIES_MIRROR` | 国内镜像           | electron-builder 辅助二进制下载地址  |
| `XDG_CACHE_HOME`                   | `~/Library/Caches` | 修改隔离环境缓存根目录               |

## 状态、恢复与清理

查看当前状态：

```bash
node scripts/macos11-env.mjs status
```

如果进程被 `kill -9`、强制关闭终端或系统意外重启，主线 `node_modules` 可能仍处于切换状态。重新打开终端后执行：

```bash
node scripts/macos11-env.mjs restore
```

确认主线 `node_modules` 已恢复并准备删除全部低版本缓存时执行：

```bash
node scripts/macos11-env.mjs clean
```

`clean` 在以下情况会拒绝执行：

- 兼容 `node_modules` 仍处于活动状态；
- 主线的 `node_modules` 备份仍存在。

这两种情况应先退出活动环境或执行 `restore`，避免误删主线依赖。

## 签名说明

隔离打包配置使用 ad-hoc 签名，适合开发和内部测试：

- 不需要 Apple Developer 证书。
- Apple Silicon 可以正常启动生成的包。
- 从网络下载到其它 Mac 后，仍可能触发 Gatekeeper 提示。

正式对外分发需要在 macOS 构建机上使用 Developer ID 签名、公证和装订票据。低版本工具链不会改变这一要求。详细流程见 [`../docs/macos-signing.md`](../docs/macos-signing.md)。

## 常见问题

| 现象                                             | 处理                                                                                          |
| ------------------------------------------------ | --------------------------------------------------------------------------------------------- |
| `Node.js 22.x is required`                       | 用 nvm/fnm/Homebrew 安装 Node 22，或让已有的 Node 22 位于脚本可发现的位置                     |
| `Cannot find module 'fs/promises'`               | 旧缓存中的 electron-builder 被旧版 Node 启动；执行 `clean` 后重新 `install`                   |
| Electron 下载超时                                | 检查网络，或修改 `ELECTRON_MIRROR` 后重跑 `install`                                           |
| 原生模块编译失败                                 | 确认 Xcode Command Line Tools、Python 3 和 Node 架构一致，然后执行 `clean` 后重装             |
| `another macOS 11 environment process is active` | 先结束现有 `dev/build/shell`；若 PID 已不存在，再执行 `restore`                               |
| `conflict; mainline and backup both exist`       | 不要删除文件，先执行 `restore` 并检查备份路径；必要时人工确认两份 `node_modules`              |
| 打包后仍提示系统版本不支持                       | 确认使用 `macos11-env.mjs build`，产物中 `Info.plist` 的 `LSMinimumSystemVersion` 应为 `11.0` |
