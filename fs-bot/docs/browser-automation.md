# 浏览器自动化（第一版）

第一版把网页操作作为客户端本地工具 `browser_op` 接入现有模型工具调用链。
模型通过页面快照里的 `ref` 操作控件，不直接使用屏幕坐标。

## 运行条件

- 电脑已安装 Microsoft Edge 或 Google Chrome。
- 首次打开网站后，如果网站要求登录，用户在自动打开的专用浏览器里完成一次登录。
- 登录态保存在 `<运行目录>/userdata/automation/profiles/<userId>/`，后续任务直接复用。

浏览器配置与用户日常使用的浏览器配置相互独立，不读取或覆盖默认 Chrome / Edge 配置。

## 可用动作

| 动作 | 主要参数 | 用途 |
| --- | --- | --- |
| `open` | `url` | 打开网址 |
| `snapshot` | `maxElements` | 获取页面、可操作控件和可见文字 |
| `click` | `ref` | 点击按钮、链接、菜单等 |
| `fill` | `ref`、`value` | 填写输入框 |
| `select` | `ref`、`value` / `label` | 选择下拉项 |
| `check` / `uncheck` | `ref` | 勾选或取消勾选 |
| `press` | `key`、`ref` | 按键或提交 |
| `scroll` | `x`、`y` | 滚动页面 |
| `upload` | `ref`、`path` | 为文件控件选择本机文件 |
| `extract` | `ref`、`maxChars` | 读取控件或整页文字 |
| `screenshot` | `fullPage` | 保存截图并返回路径 |
| `wait` | `ms` 或 `ref` | 等待时间或控件出现 |
| `back` / `reload` | - | 返回或刷新 |
| `pages` / `switch` | `index` | 查看、切换标签页 |
| `perform` | `steps` | 一次连续执行最多 30 个动作 |

`perform` 用于减少模型往返。例如：

```json
{
  "action": "perform",
  "steps": [
    { "action": "fill", "ref": "e3", "value": "张三" },
    { "action": "fill", "ref": "e5", "value": "13800000000" },
    { "action": "select", "ref": "e8", "label": "研发部" },
    { "action": "click", "ref": "e12" }
  ]
}
```

每个待执行动作都会自动等待元素可操作。批量执行结束后返回最新页面快照；
如果中间控件失效，会返回失败动作与当前页面，模型可以重新观察后继续。

## 自动处理

- 网页 `alert`、`confirm` 和离开页面确认框默认接受。
- 点击产生的新窗口会成为该对话的当前标签页。
- 页面下载会保存到自动化临时目录，并在后续快照里给出路径。
- 每次点击后会等待 DOM 稳定和网络空闲，但不会无限等待持续轮询的页面。
- 用户点击“停止生成”时，当前对话的浏览器标签页会关闭。

## 当前限制

- 页面快照只扫描主页面，暂不支持 iframe 内的控件。
- 验证码、扫码登录和 MFA 仍需要用户接管。
- 页面控件如果没有可读名称，模型可能需要先截图，再通过 `selector` 或 `text` 后备定位。
- 第一版只实现浏览器操作；Windows 桌面 UI Automation 尚未接入。
