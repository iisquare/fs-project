# macOS 打包、签名与公证

macOS 上「装得上、打得开」需要三件事，缺一步都会被 Gatekeeper 拦：

1. **签名（codesign，本机）**：给 .app 及其内部所有可执行文件逐个签上 Developer ID Application 证书，附带加固运行时（hardened runtime）与 entitlements。证明「是谁做的、内容没被改」。
2. **公证（notarytool，Apple 服务器）**：把包上传给 Apple 扫描恶意代码，通过后拿到一张票据。用户第一次打开时系统会拿这个票据核对。
3. **装订（stapler，本机）**：把票据写进 .app / .dmg，这样离线也能验。

公证不是签名，签名也不是公证；只做其中一个，用户看到的仍然是「无法验证开发者」或「Apple 无法检查其是否包含恶意软件」。

macOS 的签名与公证**只能在 macOS 上做**——electron-builder 按 `process.platform` 判断，非 darwin 上会打印「skipped macOS application code signing」并跳过后面的公证。构建机必须是 Mac。

## 当前仓库状态

| 位置                                    | 现状                           | 说明                                                                                    |
| --------------------------------------- | ------------------------------ | --------------------------------------------------------------------------------------- |
| `electron-builder.yml` → `mac.notarize` | `false`                        | **明关公证的占位值**，要公证就改成 `true`（v25 之后它是布尔开关，凭据只能从环境变量取） |
| `electron-builder.yml` → `appId`        | `com.electron.app`             | 模板占位值，签名 / 公证 / 自动更新前应换成真实反向域名（bundle id 就是它）              |
| `mac.entitlementsInherit`               | `build/entitlements.mac.plist` | 显式指向仓库里那份                                                                      |
| `mac.entitlements`                      | 未写                           | 默认就是 `build/entitlements.mac.plist`（该文件存在时），与上面那份是同一个文件         |
| `mac.hardenedRuntime`                   | 未写                           | 默认 `true`，公证要求开，别关                                                           |

也就是说：现在 `pnpm build:mac` 出来的 .app 是**未签名**的（没有证书时 electron-builder 会跳过签名并打印提示），也不会公证。

## 准备

1. Apple Developer Program 账号（个人 / 公司都行，公司账号要 D-U-N-S）。
2. 在 developer.apple.com → Certificates 建一张 **Developer ID Application** 证书（用于分发到 App Store 之外；上架 App Store 用的是另一套证书与流程），在钥匙串里导出为 `.p12` 并设口令。
3. 记下 **Team ID**（10 位，developer.apple.com 的 Membership 页）。
4. 公证凭据三选一：
   - **App Store Connect API Key（推荐）**：Keys 页建一个 App Manager 角色的密钥，下载 `.p8`（只能下一次），记 Key ID 与 Issuer ID；
   - **App 专用密码**：appleid.apple.com 生成 App-Specific Password（不是 Apple ID 登录密码）；
   - **钥匙串 profile**：本机执行一次 `xcrun notarytool store-credentials` 存好，之后只引用 profile 名。

检查证书有没有装进钥匙串（有输出就说明签名能用）：

```bash
security find-identity -v -p codesigning
```

## 打包时签名与公证（推荐）

### 凭据

macOS 侧只从环境变量取，证书按 `CSC_LINK` → 临时钥匙串的路径解析：

| 环境变量                                            | 作用                                                                                                       |
| --------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `CSC_LINK`                                          | 签名证书：`.p12` 的路径或 base64 内容（配了它，electron-builder 会建一个临时钥匙串导入并在构建结束后删掉） |
| `CSC_KEY_PASSWORD`                                  | 上面 `.p12` 的口令                                                                                         |
| `CSC_NAME`                                          | 按名字挑证书（钥匙串里有多张时用），等价于配置里的 `mac.identity`                                          |
| `CSC_KEYCHAIN`                                      | 指定使用哪个钥匙串（不给就用系统默认搜索列表）                                                             |
| `CSC_INSTALLER_LINK` / `CSC_INSTALLER_KEY_PASSWORD` | 只在做 pkg 安装包（`Developer ID Installer` 证书）时需要；本仓库不出 pkg，可以忽略                         |

公证凭据同样只从环境变量取，三组任选一组：

| 方式                      | 需要的变量                                                          | 备注                                                                                |
| ------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| App Store Connect API Key | `APPLE_API_KEY`（.p8 路径）、`APPLE_API_KEY_ID`、`APPLE_API_ISSUER` | **推荐**，CI 里最稳                                                                 |
| Apple ID + 专用密码       | `APPLE_ID`、`APPLE_APP_SPECIFIC_PASSWORD`、`APPLE_TEAM_ID`          | 三个必须同时给，缺一个直接报错；官方标为不推荐                                      |
| 钥匙串 profile            | `APPLE_KEYCHAIN_PROFILE`（可选 `APPLE_KEYCHAIN`）                   | 先在本机 `xcrun notarytool store-credentials` 存一次，适合不出网到 Apple 的凭据分发 |

本地出包：

```bash
export CSC_LINK=/Users/me/secrets/fs-bot-developer-id.p12
export CSC_KEY_PASSWORD='<p12 口令>'
export APPLE_API_KEY=/Users/me/secrets/AuthKey_XXXXXXXXXX.p8
export APPLE_API_KEY_ID=XXXXXXXXXX
export APPLE_API_ISSUER=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
pnpm build:mac
```

公证要上传到 Apple 服务器，构建机必须能出网；被代理拦住的话公证会失败并留下日志（`xcrun notarytool log <id>` 才能看详情）。

### 配置里放什么

```yaml
appId: com.iisquare.fs.bot # 换成真实反向域名，别再留 com.electron.app
mac:
  entitlements: build/entitlements.mac.plist
  entitlementsInherit: build/entitlements.mac.plist
  hardenedRuntime: true
  gatekeeperAssess: false
  type: distribution
  notarize: true # 打开公证：证书与凭据都在环境变量里，这里只是开关
```

相关字段：

| 字段               | 作用                                                                                                 |
| ------------------ | ---------------------------------------------------------------------------------------------------- |
| `identity`         | 指定签名证书名；不写就自动在钥匙串里找 `Developer ID Application`（`CSC_NAME` 也能给）               |
| `hardenedRuntime`  | 默认 `true`，公证的前提；除非做 ad-hoc 签名，否则不要关                                              |
| `gatekeeperAssess` | 是否让 `@electron/osx-sign` 跑一遍 Gatekeeper 评估，默认 `false`；开着会在签名阶段就报错，调试时有用 |
| `strictVerify`     | 默认 `true`，签名后校验包内容                                                                        |
| `type`             | `distribution`（对外分发）/ `development`                                                            |
| `signIgnore`       | 正则或正则数组，跳过某些文件的签名                                                                   |
| `binaries`         | 额外要签的二进制（比如自己放进 Resources 的可执行文件）                                              |
| `notarize`         | 是否公证；凭据缺失时只警告并跳过                                                                     |

### entitlements 里有什么

`build/entitlements.mac.plist` 现在三项，都是 Electron 的运行需要：

| 键                                                       | 为什么需要                                                 |
| -------------------------------------------------------- | ---------------------------------------------------------- |
| `com.apple.security.cs.allow-jit`                        | V8 要动态生成可执行内存；arm64 + Electron 20+ 缺它会直接崩 |
| `com.apple.security.cs.allow-unsigned-executable-memory` | 兼容老插件 / 原生模块的未签名可执行内存                    |
| `com.apple.security.cs.allow-dyld-environment-variables` | 允许通过 `DYLD_*` 注入（调试与部分原生模块需要）           |

**不要**在正式分发的包里加 `com.apple.security.cs.disable-library-validation`：只有在 ad-hoc 签名（`identity: "-"`）或混用了别的 Team ID 的预签名二进制时才需要它，正式 Developer ID 签名加它等于主动放宽校验规则。

### 构建顺序

`pnpm build:mac`（等价 `electron-vite build` + `electron-builder --mac`）里，签名的实际次序是：

1. 打包出 `dist/mac-arm64/fs-bot.app`（`--dir` 或失败时能直接看到它）；
2. 签名整个 .app（内部 helper、框架、以及 `app.asar.unpacked` 里的原生模块都会被逐个签上；`resources/**` 与 `better-sqlite3-multiple-ciphers` 都是 `asarUnpack` 的目标，所以会走到签名）；
3. 公证：把 .app 压成临时 zip → `xcrun notarytool submit --wait` → `stapler staple` 到 .app，然后删掉临时 zip（日志里能看到 `notarization successful`）；
4. 之后才生成 DMG 与 zip 两个产物（`mac` 未写 `target` 时取默认值 `zip` + `dmg`）。

**DMG 本身不公证**：electron-builder 只公证 .app，DMG 是在公证之后才做的；`dmg.sign` 默认 `false`（官方注释说明 DMG 签名与公证要求会互相冲突，别去开）。DMG 里那份 .app 已带装订好的票据，用户拖进「应用程序」后能正常打开；要给 DMG 也补一张票据，走下面的手工流程。

## 打包后手工流程（补签 / 补公证）

适用于：拿到的是一份已打包产物，或只想给 DMG 单独补公证。

### 1. 先看当前状态

```bash
codesign -dv --verbose=4 dist/mac-arm64/fs-bot.app
codesign --verify --deep --strict --verbose=2 dist/mac-arm64/fs-bot.app   # --deep 只用于验证
spctl -a -vvv -t install dist/mac-arm64/fs-bot.app
xcrun stapler validate dist/mac-arm64/fs-bot.app
```

`codesign -dv` 里应有 `Authority=Developer ID Application: ...`、`TeamIdentifier` 与 `flags=0x10000(runtime)`。

### 2. 要重签就重新打包，别手搓 codesign

.app 里嵌套着多种 helper、framework 与原生模块，必须由内向外逐个签、顺序还不能错，所以**签名一律交给 electron-builder**（内部 `@electron/osx-sign`，会按 `build/entitlements.mac.plist` 同时签主包与子包）：改了配置、换了证书、或往包里加过东西，直接重新跑 `pnpm build:mac`，别在产物上手工 `codesign --deep`（`--deep` 签名已被 Apple 标注为不适合分发）。

只有「内层没动、只是最外层 bundle 的封条需要重做」（例如改过 `Info.plist`，或给已签名的包换一张证书）时，才用一条命令重签最外层：

```bash
codesign --force --timestamp --options runtime \
  --entitlements build/entitlements.mac.plist \
  --sign "Developer ID Application: 公司名 (TEAMID)" \
  dist/mac-arm64/fs-bot.app
```

内层 helper / framework 的签名保持原样——内层内容一旦改过，这条命令救不回来，必须回到重新打包。

如果手上已经有一份**签好并公证好**的 .app，只想重出 DMG / zip，用 `--prepackaged`（它跳过打包阶段，也就跳过了签名与公证，给它的目录必须已经是完成态）：

```bash
pnpm exec electron-builder --mac --prepackaged dist/mac-arm64/fs-bot.app
```

### 3. 公证与装订

先存一次凭据（只需一次，之后按 profile 名引用）：

```bash
xcrun notarytool store-credentials "fs-bot-notary" \
  --apple-id "<Apple ID>" --team-id "<TEAMID>" --password "<App 专用密码>"
```

给 .app 补公证（改成用 `--key` / `--key-id` / `--issuer` 也行，那样不依赖钥匙串）：

```bash
mkdir -p build/notarize
ditto -c -k --keepParent dist/mac-arm64/fs-bot.app build/notarize/fs-bot.zip
xcrun notarytool submit build/notarize/fs-bot.zip --keychain-profile "fs-bot-notary" --wait
xcrun stapler staple dist/mac-arm64/fs-bot.app
```

给 DMG 补公证 + 装订：

```bash
xcrun notarytool submit dist/fs-bot-1.0.0.dmg --keychain-profile "fs-bot-notary" --wait
xcrun stapler staple dist/fs-bot-1.0.0.dmg
```

`notarytool submit` 的 JSON 输出里有 `id` 与 `status`；`status` 不是 `Accepted` 时用 `xcrun notarytool log <id> --keychain-profile "fs-bot-notary"` 看具体是哪个文件被拒。

## 验收清单

| 检查                                                               | 期望                                                                                                               |
| ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |
| `codesign -dv --verbose=4 <.app>`                                  | 有 `Authority=Developer ID Application: ...`、`TeamIdentifier`、`flags=...runtime`                                 |
| `codesign --verify --deep --strict --verbose=2 <.app>`             | 无输出（通过）                                                                                                     |
| `xcrun stapler validate <.app>`                                    | `The validate action worked!`                                                                                      |
| `xcrun stapler validate <.dmg>`                                    | 同上（只对补过公证的 DMG 有效）                                                                                    |
| `spctl -a -vvv -t install <.app>`                                  | `accepted`，`source=Notarized Developer ID`                                                                        |
| `spctl -a -vvv -t open --context context:primary-signature <.dmg>` | `accepted`                                                                                                         |
| 另一台干净 Mac 上                                                  | 从 DMG 拖进「应用程序」后双击直接打开，不弹「无法验证开发者」；系统设置 → 隐私与安全性里没有「仍要打开」的待办条目 |
| 自动更新                                                           | 更新包是签名且公证过的（electron-updater 的 mac 通道走 zip 产物，更新包不改签名规则）                              |

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
| 长期有效             | 只对本次安装生效：重新下载、重装、以及换机器都会回到未放行的状态；升级包也要各自重签一遍                                                                                                         |

**什么时候可以用**：拿到一份未签名的测试包、或者自己机器上那份安装的签名坏了（比如往 .app 里写过东西），要立刻验证功能。用完请记住它只是本机状态。

**正式解法**永远是把三件事补齐：Developer ID Application 证书签名 → 公证 → 装订。本项目只要按前面「打包时签名与公证」把 `mac.notarize` 打开、把凭据放进环境变量，`pnpm build:mac` 出来的包就不需要任何人执行这条命令。

## 常见问题

| 现象                                                                 | 原因与处理                                                                                                                                                                                                           |
| -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `code object is not signed at all` / `code signature invalid`        | 没签，或签名之后又改了 .app 里的文件。**这是本项目特别容易踩的坑**：运行目录绝不能落在 .app 内部（见 `../CLAUDE.md` 里「不往安装目录里写」那一段），往包里写配置 / 数据库会直接破坏签名；改完内容只能重新签          |
| 用户报「已损坏，无法打开」/「无法验证开发者」                        | 正式解法是签名 + 公证 + 装订；只有在自己机器上验证测试包时才用 `xattr -cr` + ad-hoc 重签（见「本机临时放行」）——那是本机状态，换机、重新下载、对外分发一律无效                                                       |
| `resource fork, Finder information, or similar detritus not allowed` | 包内残留扩展属性（Finder 信息、资源分支），`codesign` 拒绝签名。先 `xattr -cr <路径>` 再签                                                                                                                           |
| 用户看到「Apple 无法检查其是否包含恶意软件」                         | 公证过了但票据没装订（下载后被解压 / 打散），或根本没过公证。检查 `stapler validate`，不行就重跑提交                                                                                                                 |
| `errSecInternalComponent`                                            | 钥匙串锁定或证书不可访问：先 `security unlock-keychain`，导入 .p12 时要带 `-T /usr/bin/codesign` 与 `security set-key-partition-list`（electron-builder 用 `CSC_LINK` 建临时钥匙串时会自己做这两步，手工导入容易漏） |
| 签名时 `errSecInternalComponent` 只出现在 CI 无人值守的机器上        | 登录钥匙串没解锁；用 `CSC_LINK` 走临时钥匙串这条路的方案更省事                                                                                                                                                       |
| 公证报 `The signature does not include a secure timestamp`           | 签名时漏了 `--timestamp`（electron-builder 默认会加，手工签时容易忘）                                                                                                                                                |
| 公证报某个二进制未签名                                               | `.app` 里塞了额外的可执行文件（原生模块、自己的 helper）。把它加进 `mac.binaries`，或确认它落在 `app.asar.unpacked` 下                                                                                               |
| `notarytool` 报 401 / `authentication failed`                        | 用了 Apple ID 登录密码而不是 App 专用密码；API Key 则核对 Key ID 与 Issuer ID；三组凭据混搭也会报错                                                                                                                  |
| 公证一直 `In Progress`                                               | 首次公证可能要几分钟，`--wait` 会一直等；网络被代理拦就会一直卡住，检查出网                                                                                                                                          |
| Windows 上跑 `pnpm build:mac`                                        | 不会签名也不会公证（非 darwin 直接跳过），产物只能在 macOS 上出                                                                                                                                                      |
