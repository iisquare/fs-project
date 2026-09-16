# file

基于Minio实现的图库及文件管理系统。

## 功能特性
- 极速秒传
- 分段上传
- 分段下载
- 客户端直传

## 图片访问

| 路径 | 鉴权 | 输出 |
| --- | --- | --- |
| GET `/image/{id}[-参数]{后缀}` | `sharable=1`，匿名访问 | 缩放、转码、可加水印的分享图 |
| GET `/raw/{id}{后缀}?time=&expire=&token=` | 时效校验码 | 原图字节，`inline` 内联展示，不缩放、不转码、不加水印 |
| GET `/file/{id}{后缀}?time=&expire=&token=` | 时效校验码 | 原图字节，`attachment` 下载 |

- 图片参数以 `-` 拼接在文件标识与后缀之间，支持 `w` 宽度、`h` 高度、`c` 剪裁缩放、
  `m` 水印开关、`t` 无水印校验码、`q` 图片质量。
- 时效校验码由 `/file/url` 生成（`type` 取 `file`、`raw`、`image`），
  `token` 与文件标识及起止时间绑定，`expire` 默认 300000 毫秒，由 `fs.file.shareKey` 参与计算。
- `fs.file.wmEnable` 为水印总开关，关闭后 `/image/` 不再添加水印；
  单个请求若需无水印图，走 `t` 校验码（由 `/file/url` 传入 `token: true` 生成）。
- `/raw/` 仅开放常见栅格图片类型，内联渲染带脚本的 SVG 等同源 XSS 风险较高，故不放开。

### 参考
