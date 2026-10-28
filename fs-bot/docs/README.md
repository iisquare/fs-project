# docs

跨模块的流程文档放这里，与代码一起走 Git。模块内部的约定仍写在代码旁边的 README 里：

| 位置                                 | 内容                                                                      |
| ------------------------------------ | ------------------------------------------------------------------------- |
| `../CLAUDE.md`                       | 项目结构、数据层（配置 / 用户库 / 图标 / 技能）、工具与对话循环的整体说明 |
| `../scripts/README.md`               | 开发期脚本与用户库 schema 的升级 / 排错手册                               |
| `../src/main/database/sql/README.md` | SQL 文件命名与编写约定                                                    |
| `./`（本目录）                       | 与具体模块无关的流程文档：打包产物的代码签名、客户端依赖的服务端开通步骤  |

## 文档清单

| 文档                                         | 内容                                                                                                     |
| -------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| [backend-services.md](./backend-services.md) | 客户端依赖的服务端与上线开通步骤（用户权限 / 用户积分 / 密钥）                                           |
| [autostart.md](./autostart.md)               | 开机启动的开发 / 生产差异、系统状态读取与 Windows 安装卸载清理                                           |
| [windows-signing.md](./windows-signing.md)   | Windows 侧签名：证书怎么选、electron-builder 怎么配、打包后用 signtool 补签与验证                        |
| [macos-signing.md](./macos-signing.md)       | macOS 侧签名 + 公证 + 装订：证书与凭据准备、配置改动、打包步骤、验收清单，全部在 `pnpm build:mac` 里完成 |

## 通用约定

- **签名凭据不进仓库。** 证书文件、证书口令、API Key、App 专用密码一律只从进程环境变量取（本地 shell 临时导出、CI 的 secret 存储），`electron-builder.yml` 里只留非机密项（`publisherName`、时间戳服务器、是否强制签名）。证书本体也不要提交，只提交「读取哪个环境变量」这件事。
- **`.env*` 不是秘密存放处。** electron-vite 会把 `.env` 文件里所有 `VITE_` 前缀的变量内联进渲染进程产物（`.env.production` 里的 `VITE_DB_SECRET` 就是这么用的），打包后从 asar 里能直接读出来。签名口令、证书密码绝不要写进 `.env*`。
- **产物都在 `dist/`。** Windows 是 NSIS 安装包 `dist/fs-bot-1.0.0-setup.exe`；macOS 在 `mac` 未指定 `target` 时取 Electron 框架的默认值，出 `dist/fs-bot-1.0.0.dmg` 与 `dist/fs-bot-1.0.0-mac.zip`。未打包的目录是 `dist/win-unpacked/`、`dist/mac/`、`dist/mac-arm64/`，补签时用的就是它们。
- **签名受平台限制。** macOS 的签名与公证只能在 macOS 上做（electron-builder 直接按 `process.platform` 判断，非 darwin 会打印「skipped macOS application code signing」）；Windows 的签名可以在 Windows 上做，也可以在 macOS / Linux 上借 wine + osslsigncode 完成。CI 选机按这条排。
- **命令的输出保持 ASCII。** 文档本身用中文，但脚本、日志、自写命令的输出一律英文 / ASCII（`->` 而不是 `→`）：Windows 控制台默认 GBK 代码页，UTF-8 中文会显示成乱码。这条与 `../scripts/README.md` 一致。
- **配置项名称以当前 `electron-builder@26.15.3` 为准。** Windows 的签名参数收在 `win.signtoolOptions` / `win.azureSignOptions` 之下（v24 及更早是直接挂在 `win` 上）；`mac.notarize` 在 v25 之后是一个布尔开关，凭据只能走环境变量。升级 electron-builder 大版本时先回这两篇文档核对键名。
