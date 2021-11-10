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
