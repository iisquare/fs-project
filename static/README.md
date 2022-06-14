# 静态资源

## NVM
- 查看可用版本
```
nvm list
```
- 安装Node.js环境
```
nvm install x.x.x
```
- 临时切换Node.js版本
```
nvm use x.x.x
```
- 切换默认Node.js版本
```
nvm alias default vx.x.x
```
- 删除Node.js环境
```
nvm uninstall x.x.x
```

## 依赖管理
```
yarn add [package]@[version] # dependencies
yarn add -D [package]@[version] # devDependencies
yarn add --dev [package]@[version] # devDependencies
yarn upgrade [package]@[version]
yarn upgrade –latest [package]
yarn remove <package...>
```

## 反向代理

### Nginx
```
location /uri/ {
  # /uri/index.html -> /path/to/static/uri/index.html
  # root /path/to/static/;

  # /uri/index.html -> /path/to/static/index.html
  # alias /path/to/static/;

  # gzip on;

  # ETag: "文件大小的十六进制"; Last-Modified: UTC DateTime; Status Code: 304 OK
  # etag on;

  # exact：完全符合; before：响应的修改时间小于或等于请求头中的 “If-Modified-Since” 字段的时间
  # if_modified_since off | exact | before;

  # 在缓存期内不会请求服务端，更不会触发ETag判断。Status Code: 200 OK (from disk/memory cache)
  # expires 30d;

  # autoindex on;
  # autoindex_localtime on;
  # autoindex_exact_size off;

}
```
