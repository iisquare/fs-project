# Windows 打包与签名

未签名的 exe：SmartScreen 弹「Windows 已保护你的电脑」、企业侧可能被下载策略直接拦掉、杀软对安装包更敏感。本仓库当前 `electron-builder.yml` 的 `win` 段只有 `executableName`，**打出来的安装包与程序都没有签名**——这篇讲怎么补上，以及签名应该在打包的哪一步发生。

配置项名称以仓库里实际装的 `electron-builder@26.15.3` 为准：签名参数收在 `win.signtoolOptions` / `win.azureSignOptions` 之下（v24 及更早直接挂在 `win` 上）。

## 先决定证书从哪来

2023-06 起 CA/B 基线要求代码签名私钥必须落在 FIPS 140-2 Level 2 以上的硬件或云服务里，所以「官网下载一个 .pfx 随便存盘」的做法已经没有合规来源了。四种现实选项：

| 方式                                                     | 说明                                                                                                                | 适配场景                                      |
| -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| Azure Trusted Signing                                    | 微软自家服务，签名在云端完成，私钥不落地；electron-builder 26 内置 `win.azureSignOptions`，不需要额外安装签名客户端 | **新项目首选**，月费低、配置最少              |
| 云 HSM（DigiCert KeyLocker / SSL.com eSigner / Sectigo） | 私钥在厂商云上，用厂商的签名客户端或 dlib；electron-builder 走 `signtoolOptions.sign` 自定义函数接                  | 已在这些 CA 建档、要 EV 信誉                  |
| 硬件令牌 / 本地 HSM                                      | 证书装进 Windows 证书存储，用 `certificateSubjectName` / `certificateSha1` 指定用哪一张                             | 已有 EV 令牌，且固定在一台 Windows 机器上出包 |
| `.pfx` 文件                                              | 只作测试用；合规证书不再以这种形式交付                                                                              | 本地验证流程，别用于对外发布                  |

不管走哪条，都先把这两件事记下来：

- **证书的 CN（Common Name）**：它要一字不差地填进 `publisherName`，也是安装包属性里显示的发布者。
- **证书指纹（SHA1 thumbprint）**：证书存储签名方式需要它来指定用哪一张证书。

PowerShell 里看证书存储：

```powershell
Get-ChildItem Cert:\CurrentUser\My, Cert:\LocalMachine\My |
  Where-Object { $_.HasPrivateKey } |
  Select-Object Subject, Thumbprint, NotAfter
```

## 打包时签名（推荐）

### 签名发生在哪两步

electron-builder 在 Windows 上会签两次，缺一不可：

1. **程序本身**：pack 阶段先给 `dist/win-unpacked/fs-bot.exe` 用 `resedit` 写入图标 / 版本信息 / 执行级别，然后对它签名（顺序反了签名会被改坏，所以不要手工去改已签名的 exe）。
2. **安装包**：NSIS 把 `win-unpacked/` 里的内容压进 `dist/fs-bot-1.0.0-setup.exe` 之后，再对安装包本身签名。

这就是「打包后补签」为什么不等于「打包时签名」：安装包里的 `fs-bot.exe` 是打包当时烙进去的，事后只签 `setup.exe`，装出来的程序依旧是未签名的（用户从开始菜单启动时照样可能被拦）。能改配置就别走补签。

### 凭据只走环境变量

证书与口令按下面的顺序解析，命中最靠前的一条就停止：

| 顺序 | 证书来源                                                                                        | 配套口令                                                       |
| ---- | ----------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| 1    | `signtoolOptions.certificateSubjectName` / `certificateSha1`（从证书存储取，仅 Windows）        | —                                                              |
| 2    | `signtoolOptions.certificateFile`                                                               | `signtoolOptions.certificatePassword` → `WIN_CSC_KEY_PASSWORD` |
| 3    | `cscLink`（`win.cscLink`，再退到顶层 `cscLink`）→ 环境变量 `WIN_CSC_LINK` → 环境变量 `CSC_LINK` | `win.cscKeyPassword` / `cscKeyPassword` → `CSC_KEY_PASSWORD`   |

`CSC_LINK` / `WIN_CSC_LINK` 可以是 base64 的 pfx 内容，也可以是 .pfx 文件的路径（必要时还会被临时释放到工作目录）。本地出包最省事的写法是设两个环境变量：

```powershell
$env:WIN_CSC_LINK = 'D:\secrets\fs-bot-codesign.pfx'
$env:WIN_CSC_KEY_PASSWORD = '<口令>'
pnpm build:win
```

CI 里则把 pfx 的 base64 文本放进 secret，构建时导出到 `WIN_CSC_LINK`。

### 配置里放什么

`electron-builder.yml` 的 `win` 段只留非机密项：

```yaml
win:
  executableName: fs-bot
  forceCodeSigning: true # 签名失败就让构建失败，别静默吐出一个没签名的包
  signtoolOptions:
    publisherName: 上海 xx 信息技术有限公司 # 必须与证书 CN 完全一致
    signingHashAlgorithms: [sha256]
    rfc3161TimeStampServer: http://timestamp.digicert.com
```

`signtoolOptions` 的可用字段：

| 字段                                      | 作用                                                                                                   |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| `certificateFile` / `certificatePassword` | 直接写 pfx 路径与口令（能用环境变量就别写这里）                                                        |
| `certificateSubjectName`                  | 用证书存储里的某张证书，按 CN 匹配；EV 证书通常必须这么指定。**仅 Windows 有效**                       |
| `certificateSha1`                         | 同上，按指纹匹配；同一张 CN 有多张证书时用它区分。**仅 Windows 有效**                                  |
| `publisherName`                           | 签名后写进文件版本信息 / 安装包的发布者名；不给则取证书 CN                                             |
| `signingHashAlgorithms`                   | 默认 `[sha1, sha256]` 双签。只面向 Windows 10 及以上时给 `[sha256]` 就够（msi 与 appx 由工具自己决定） |
| `timeStampServer`                         | 老式时间戳接口，配合 sha1 那轮用（`/t`）                                                               |
| `rfc3161TimeStampServer`                  | RFC 3161 时间戳地址，默认 `http://timestamp.digicert.com`（`/tr`）                                     |
| `additionalCertificateFile`               | 额外附带的证书文件（比如中间证书链）                                                                   |
| `sign`                                    | 自定义签名函数 / 模块路径，见下                                                                        |
| `signExts`（在 `win` 下）                 | 额外要签的文件名后缀；`signExecutable: false` 则只做资源编辑不签名                                     |

**时间戳必须成功**：证书过期后，只有带可信时间戳的签名才继续有效。构建机要能访问时间戳服务器（`ELECTRON_BUILDER_OFFLINE=true` 会跳过时间戳，只适合离线验证）。

### Azure Trusted Signing

`azureSignOptions` 与 `signtoolOptions` 互斥，两个都写时走 Azure。配置：

```yaml
win:
  forceCodeSigning: true
  azureSignOptions:
    endpoint: https://eus.codesigning.azure.net
    codeSigningAccountName: fs-signing
    certificateProfileName: fs-bot
    publisherName: 上海 xx 信息技术有限公司
    timestampRfc3161: http://timestamp.acs.microsoft.com
```

鉴权走 Microsoft Entra ID 的标准环境变量，构建机上设置：

```
AZURE_TENANT_ID=<目录(租户) ID>
AZURE_CLIENT_ID=<应用注册的客户端 ID>
AZURE_CLIENT_SECRET=<客户端密码>
```

`endpoint` 要用证书配置文件所在区域的地址，`publisherName` 同样必须与证书主体一致。

### 云 HSM / 自定义签名客户端

用 `win.signtoolOptions.sign` 指到一个 JS 模块（相对路径、包名都行），electron-builder 会用它替代内置的 signtool 调用：

```js
// build/sign-windows.cjs
const { execFileSync } = require('node:child_process')

// configuration.path 待签文件；configuration.hash 本轮算法（sha1 / sha256）；
// configuration.isNest 首轮为 false、第二轮及以后为 true；
// configuration.computeSignToolArgs(true) 拿内置 signtool 参数（要复用默认行为时用）
module.exports = async function sign(configuration, packager) {
  execFileSync('C:\\Program Files\\SigningClient\\sign.exe', [
    '--file',
    configuration.path,
    '--alg',
    configuration.hash
  ])
}
```

如果厂商客户端是「输入一个文件、输出到另一个文件」的模式，就把输出路径写到 `configuration.resultOutputPath`——electron-builder 会用那个文件替换 `configuration.path`（双签时两轮都走同一个函数）。

## 打包后补签（signtool 流程）

只在拿不到构建配置权限、或要修一个已经发出去的包时用。先找到 signtool：装 Windows Kits 的机器用 SDK 里的那份，没装的话用 electron-builder 自己缓存的工具链：

```powershell
Get-ChildItem "$env:LOCALAPPDATA\electron-builder\Cache\winCodeSign" -Recurse -Filter signtool.exe |
  Select-Object -First 3 FullName
```

### 给单个文件签名

pfx 方式（`/fd` 指定文件摘要算法，`/tr` + `/td` 用 RFC 3161 时间戳）：

```powershell
signtool sign /fd sha256 /tr http://timestamp.digicert.com /td sha256 `
  /f D:\secrets\fs-bot-codesign.pfx /p "<口令>" `
  dist\fs-bot-1.0.0-setup.exe
```

证书存储 / 硬件令牌方式（用指纹挑证书）：

```powershell
signtool sign /sha1 <证书指纹> /fd sha256 /tr http://timestamp.digicert.com /td sha256 `
  dist\fs-bot-1.0.0-setup.exe
```

需要同时兼容老系统时才做双签：先 sha1（老式 `/t` 时间戳）再 sha256。

### 验证

```powershell
signtool verify /pa /v dist\fs-bot-1.0.0-setup.exe

Get-AuthenticodeSignature dist\fs-bot-1.0.0-setup.exe |
  Format-List Status, SignerCertificate, TimeStamperCertificate
```

`Status` 要是 `Valid`，并且 `TimeStamperCertificate` 不为空（时间戳签上了）。对 `dist\win-unpacked\fs-bot.exe` 也跑一遍——它是用户实际启动的那个文件。

### 内部 exe 还没签的补救

先给解包目录里的程序补签，再用 `--prepackaged` 拿这个目录重出安装包（跳过打包阶段，直接用目录里现成的内容），最后签一次新的安装包：

```powershell
signtool sign /fd sha256 /tr http://timestamp.digicert.com /td sha256 `
  /f D:\secrets\fs-bot-codesign.pfx /p "<口令>" dist\win-unpacked\fs-bot.exe

pnpm exec electron-builder --win --prepackaged dist\win-unpacked

signtool sign /fd sha256 /tr http://timestamp.digicert.com /td sha256 `
  /f D:\secrets\fs-bot-codesign.pfx /p "<口令>" dist\fs-bot-1.0.0-setup.exe
```

`--prepackaged` 的对象必须是「已经装好、能直接运行的应用目录」（就是 `win-unpacked` 那种形态）；这条路径会跳过整个打包阶段（`resedit` 的资源编辑与内置签名都不再执行），所以手工补上的签名会原样进到安装包里。反过来说，程序文件在签名之后不要再改动，改一下（哪怕只是资源里改个版本号），签名立刻失效。

## 验收清单

| 检查                              | 期望                                                                                            |
| --------------------------------- | ----------------------------------------------------------------------------------------------- |
| `signtool verify /pa /v` 对安装包 | `Successfully verified`，链完整                                                                 |
| 同上对 `win-unpacked\fs-bot.exe`  | 同样通过                                                                                        |
| `Get-AuthenticodeSignature`       | `Status = Valid`，发布者名与证书 CN 一致                                                        |
| 时间戳                            | `TimeStamperCertificate` 非空                                                                   |
| 干净机器（无开发者环境）双击安装  | 不弹 SmartScreen 的「未知发布者」；出现「Windows 已保护你的电脑」说明签名没生效或被拦在下载环节 |
| 安装后的程序属性 → 数字签名       | 有签名页，能查看证书链                                                                          |

## 常见问题

| 现象                                                                             | 原因与处理                                                                                                                  |
| -------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| 构建日志里 `no signing info identified, signing is skipped`                      | 没有任何证书来源命中：检查 `WIN_CSC_LINK` / `CSC_LINK` 是否在当前 shell 里（`Get-Item Env:WIN_CSC_LINK`），以及路径是否可读 |
| `Cannot extract publisher name from code signing certificate`                    | 从证书里读不出 CN（口令错、pfx 损坏），或证书格式不被工具链支持。临时办法是显式配 `publisherName`                           |
| 安装包签名成功，但安装后程序仍显示未签名                                         | 只补签了 `setup.exe`，内部 exe 没签。按「内部 exe 还没签的补救」走一遍                                                      |
| `Get-AuthenticodeSignature` 报 `NotSigned` / `HashMismatch`                      | 签名后文件被改过（改版本信息、替换资源、重新压缩）；重新签                                                                  |
| 时间戳失败导致整体签名失败                                                       | 构建机到时间戳服务器的出网被拦；换一个可达的 RFC 3161 服务，或确认不是 `ELECTRON_BUILDER_OFFLINE=true`                      |
| 签名成功但 SmartScreen 仍警告                                                    | 新证书没有信誉积累，是 EV / Trusted Signing 也会经历的一段；确认发布者名正确并冲量即可，不是配置错误                        |
| Windows 上跑 `pnpm build:mac`，日志出现 `skipped macOS application code signing` | 平台判断直接跳过签名与公证；macOS 产物只能在 macOS 上签                                                                     |
