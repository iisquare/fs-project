# 开机启动

开机启动以操作系统登录项为唯一事实源，不再在 `userdata/config.toml` 中保存 `autoStart`。
设置页每次打开都会读取真实登录项；开关成功后才更新界面，避免配置和系统状态各说各话。

## 开发与生产

- `pnpm dev` / `pnpm start` 不注册登录项。开发模式的 `process.execPath` 是 Electron 运行时，
  写进系统启动项后无法正确找到应用入口，因此设置页会禁用开关并说明原因。
- 只有 `app.isPackaged` 的 Windows 安装版会写入当前 `fs-bot.exe` 与固定参数 `--autostart`。
- 首次安装默认关闭；用户主动打开后才创建启动项。
- Windows 的 Run 项名固定为 `fs-bot`，升级时只更新其中的安装路径，不改变用户选择。
- 用户在 Windows「设置 > 应用 > 启动」中禁用 fs-bot 时，设置页显示“已写入但被系统禁用”，
  应用不会在下次启动时擅自重新启用。

## 卸载

Windows NSIS 的 `build/installer.nsh` 会执行以下处理：

- 覆盖安装 / 自动升级：保留 Run 项，并更新为新的安装路径。
- 真正卸载：删除 `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` 下的 `fs-bot`。
- 同时删除 `StartupApproved\Run` / `StartupApproved\Run32` 中同名开关状态，避免 Windows
  「启动应用」列表留下失效条目。

可用下面的命令检查当前用户状态：

```powershell
Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run' |
  Select-Object fs-bot

Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\Run' |
  Select-Object fs-bot
```
