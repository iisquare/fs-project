# macOS 打包、签名与公证

`pnpm build:mac` 一次产出可以直接发给别人的 DMG：里面的 .app 已经**签名**（Developer ID Application 证书）、已经**公证**（Apple 扫过一遍）、票据已经**装订**进包里。别人拖进「应用程序」双击就能打开。

三件事缺一不可：签名证明「是谁做的、内容没被改」；公证是 Apple 的服务端扫描（通过后给一张票据）；装订把票据写进包里（离线也能验）。签名和公证都需要 Apple Developer Program 会员与 Developer ID Application 证书，而且**只能在 macOS 上做**——非 darwin 上 electron-builder 会打印 `skipped macOS application code signing` 直接跳过。

## 1. 准备（只做一次）

1. 加入 Apple Developer Program（$99/年）。在 developer.apple.com → Certificates 建一张 **Developer ID Application** 证书，从钥匙串导出成 `.p12` 并设口令。
2. 记下 **Team ID**（Membership 页，10 位）。
3. 准备公证凭据，三选一：

| 方式                                  | 需要的环境变量                                                          | 备注                                                                                                                          |
| ------------------------------------- | ----------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| **App Store Connect API Key（推荐）** | `APPLE_API_KEY`（.p8 文件路径）、`APPLE_API_KEY_ID`、`APPLE_API_ISSUER` | Keys 页建一个 App Manager 角色的密钥，`.p8` 只能下载一次                                                                      |
| 钥匙串 profile                        | `APPLE_KEYCHAIN_PROFILE`（可选 `APPLE_KEYCHAIN`）                       | 先存一次：`xcrun notarytool store-credentials "fs-bot-notary" --apple-id <账号> --team-id <TEAMID> --password <App 专用密码>` |
| Apple ID + App 专用密码               | `APPLE_ID`、`APPLE_APP_SPECIFIC_PASSWORD`、`APPLE_TEAM_ID`              | 三个必须同时给；专用密码在 appleid.apple.com 生成，不是登录密码                                                               |

4. 确认证书已装进钥匙串（有输出就说明能用）：

```bash
security find-identity -v -p codesigning
```

## 2. 配置（只做一次）

`electron-builder.yml` 的 mac 段：

```yaml
appId: com.iisquare.fs.bot # 换成真实反向域名，别留模板值 com.electron.app
mac:
  entitlements: build/entitlements.mac.plist
  entitlementsInherit: build/entitlements.mac.plist
  hardenedRuntime: true
  notarize: true # 打包时公证；凭据在环境变量里，这里只是开关
```

`build/entitlements.mac.plist` 已经在仓库里，内容是 Electron 需要的那几项（`allow-jit` 等），不用改。正式分发**不要**加 `com.apple.security.cs.disable-library-validation`——那只有在 ad-hoc 签名或混用别的 Team ID 时才需要。

证书与口令不要写进配置文件、也不要写进 `.env*`（electron-vite 会把 `.env` 里 `VITE_` 开头的变量内联进产物），只走环境变量。

## 3. 每次打包

在 macOS 构建机上导出凭据，然后照常打包：

```bash
export CSC_LINK=/Users/me/secrets/fs-bot-developer-id.p12   # .p12 的路径或 base64 内容
export CSC_KEY_PASSWORD='<p12 口令>'
export APPLE_API_KEY=/Users/me/secrets/AuthKey_XXXXXXXXXX.p8
export APPLE_API_KEY_ID=XXXXXXXXXX
export APPLE_API_ISSUER=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
pnpm build:mac
```

用钥匙串 profile 的话，把后三个 `APPLE_*` 换成 `export APPLE_KEYCHAIN_PROFILE=fs-bot-notary`。

日志里应依次出现 `signing` 与 `notarization successful`——公证就是打包流程里的一步：把 .app 压成临时 zip 提交 Apple、等结果、再把票据装订到 .app。产物：

| 文件                        | 说明                                      |
| --------------------------- | ----------------------------------------- |
| `dist/mac-arm64/fs-bot.app` | 终态 .app（已签名 + 已公证 + 已装订）     |
| `dist/fs-bot-1.0.0.dmg`     | 发给用户的镜像，里面的 .app 就是上面那份  |
| `dist/fs-bot-1.0.0-mac.zip` | 自动更新用的包（electron-updater 走这个） |

## 4. 验收

```bash
codesign -dv --verbose=4 dist/mac-arm64/fs-bot.app   # 有 Authority=Developer ID Application 与 flags=...runtime
codesign --verify --deep --strict --verbose=2 dist/mac-arm64/fs-bot.app
xcrun stapler validate dist/mac-arm64/fs-bot.app     # The validate action worked!
spctl -a -vvv -t install dist/mac-arm64/fs-bot.app   # accepted, source=Notarized Developer ID
```

最后一步要在**另一台干净的 Mac** 上做：把 DMG 拷过去，拖进「应用程序」，双击打开，不弹「无法验证开发者」。自己机器上能开不代表别人的机器能开。

## 5. 可选：给 DMG 也补一张票据

electron-builder 只公证 .app，DMG 是公证之后才做的，所以镜像自己没有票据（首次挂载时可能被提示未公证）。发给外部用户前补两条命令：

```bash
xcrun notarytool submit dist/fs-bot-1.0.0.dmg --keychain-profile "fs-bot-notary" --wait
xcrun stapler staple dist/fs-bot-1.0.0.dmg
```

用 API Key 那组凭据时，把 `--keychain-profile "fs-bot-notary"` 换成 `--key <p8 路径> --key-id <Key ID> --issuer <Issuer ID>`。不需要给 DMG 单独签名，也不用解开镜像——Apple 校验的是镜像里的代码。

## 本机临时放行：`xattr -cr` + ad-hoc 重签

```bash
sudo xattr -cr /Applications/你的App.app && sudo codesign --force --deep --sign - /Applications/你的App.app
```

这条命令是**在自己机器上把一份没签名（或签名被弄坏）的包强行跑起来**的应急手段，与「对外分发」不是一回事：它没有证书、没有公证、没有票据，只解决你这台机器。逐段拆开看：

| 片段                | 作用                                                                                                                                                                                              |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `sudo`              | 改的是 `/Applications` 里那份安装；如果这个 app 归你本人所有，通常不需要 `sudo`                                                                                                                   |
| `xattr -c`          | 清空该文件的**所有**扩展属性（`-r` 表示递归到包内每个文件）。关键是其中的 `com.apple.quarantine`——浏览器下载后系统给包打的「隔离」标记，**Gatekeeper 只对带这个标记的包做检查**，标记没了就不再拦 |
| `codesign --force`  | 覆盖已有的（可能是坏的）签名；不加 `--force` 会对已签名的包直接报错                                                                                                                               |
| `codesign --deep`   | 连包内嵌套的 helper、framework 一起重签。Apple 已把用于**签名**的 `--deep` 标为不可靠（嵌套代码的授权/约束可能不对），它只适合验证                                                                |
| `codesign --sign -` | `-` 是 ad-hoc 签名的写法：不挂任何证书，只让代码签名在内容层面自洽。macOS 11+ 的 arm64 二进制必须有签名才能运行，被改动过的 bundle 也会因签名失效而报「已损坏」，ad-hoc 正好把这层兜住            |

顺序不能反：包内残留的扩展属性（Finder 信息、资源分支等「detritus」）会让 `codesign` 直接报 `resource fork, Finder information, or similar detritus not allowed`，所以惯例是先 `xattr -cr` 再签。

**它做到了什么**：隔离标记被清掉 → Gatekeeper 不再过问；签名自洽 → 系统不再报「已损坏，无法打开」；这台机器上双击能开。

**它做不到什么**（这才是重点）：

| 事项                 | 说明                                                                                                                                                                                             |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 换一台机器           | 无效。别人拿到的包该拦还是拦，得让对方重复同一条命令（所以内部测试同学要各自跑一遍）                                                                                                             |
| 重新下载 / 解压      | 无效。`com.apple.quarantine` 会重新写上，回到原状                                                                                                                                                |
| 对外分发             | 不行。ad-hoc 签名不能公证（Apple 只受理 Developer ID 签名的包），用户照样看到「无法验证开发者」                                                                                                  |
| 已装订的公证票据     | 失效。票据与签名绑定，重签之后 `xcrun stapler validate` 会失败，得重新公证                                                                                                                       |
| 加固运行时与子包授权 | 这条命令没带 `--options runtime`、也没带 entitlements，等于把 electron-builder / `@electron/osx-sign` 由内向外逐层签好的那套约束一起换掉——它「能跑」正是因为约束被拆掉了，而不是因为打包被修好了 |
| 长期有效             | 只对本次安装生效：重新下载、重装、以及换机器都会回到未放行的状态                                                                                                                                 |

**什么时候可以用**：拿到一份未签名的测试包，或者自己机器上那份安装的签名坏了（比如往 .app 里写过东西），要立刻验证功能。用完请记住它只是本机状态——正式解法永远是上面那三步。

## 常见问题

| 现象                                                | 原因与处理                                                                                                                                                                      |
| --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 打包日志里 `skipped macOS application code signing` | 没找到签名证书：用 `security find-identity -v -p codesigning` 确认证书在钥匙串里，再确认 `CSC_LINK` / `CSC_NAME` 是在当前 shell 里导出的                                        |
| `errSecInternalComponent`                           | 钥匙串锁定或证书不可访问；用 `CSC_LINK` 让 electron-builder 建临时钥匙串（它会自动完成导入与解锁）比手工往登录钥匙串里导更省事                                                  |
| 用户报「已损坏，无法打开」                          | 签名被破坏或压根没签。**本项目特别容易踩**：运行目录绝不能落在 .app 内部（见 `../CLAUDE.md` 里「不往安装目录里写」那一段），往包里写配置 / 数据库会直接让签名失效，只能重新打包 |
| 用户看到「Apple 无法检查其是否包含恶意软件」        | 没公证，或票据没装订（下载后被解压打散）。检查 `xcrun stapler validate`，不行就重新打包                                                                                         |
| 公证报 401 / `authentication failed`                | 用了 Apple ID 登录密码而不是 App 专用密码；API Key 则核对 Key ID 与 Issuer ID；三组凭据混搭也会报错                                                                             |
| 公证一直 `In Progress`                              | 首次公证可能要几分钟，构建机会一直等；网络被代理拦住会卡死，检查出网                                                                                                            |
| Windows 上跑 `pnpm build:mac`                       | 不会签名也不会公证（非 darwin 直接跳过），macOS 产物只能在 macOS 上出                                                                                                           |
