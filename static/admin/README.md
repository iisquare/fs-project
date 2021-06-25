# Admin based on ant-design-pro-vue

## 安装和运行
- 安装依赖
```
yarn install
```

- 开发模式运行
```
yarn run serve
```

- 编译项目
```
yarn run build
```

- Lints and fixes files
```
yarn run lint
```

- 依赖管理
```
yarn add [package]@[version] # dependencies
yarn add -D [package]@[version] # devDependencies
yarn add --dev [package]@[version] # devDependencies
yarn upgrade [package]@[version]
yarn upgrade –latest [package]
yarn remove <package...>
```

- 跨域请求
```
# 将API接口地址设置为本地路径
VUE_APP_API_BASE_URL=./
# 配置代理托管远程接口调用
proxy: {
  '/proxy': {
    target: 'http://ip:port',
    ws: false,
    changeOrigin: true
  }
}
```

## 注意事项

### 表单在1.5.0-rc.3版本正式发布FormModel相关文档，项目统一采用v-model双向绑定方式进行开发。

### 表格的分页暂不支持深度监听，只能重置影响分页数据，[issue](https://github.com/vueComponent/ant-design-vue/issues/70)。

### 排序可通过config接口获取排序项，通过下拉菜单方式进行选择。

### 在ant-design-vue-1.7.3中，若FormItem的isFormItemChildren为true时，wrapperCol不生效。
```
provide: function provide() {
  return {
    isFormItemChildren: true
  };
},
```
因此，当嵌套表单或在a-form-model-item中提供Modal弹窗填写表单，则wrapperCol无法正常工作。此时，可在自己的组件中，提供provide函数，强制将isFormItemChildren覆盖为false即可。

## 最佳实践

### 当有多个属性值被子组件修改时，可采用[.sync](https://cn.vuejs.org/v2/guide/components-custom-events.html#sync-%E4%BF%AE%E9%A5%B0%E7%AC%A6)修饰符。

### 深度嵌套的子组件，访问父组件的部分内容，可采用[Provide / Inject](https://v3.cn.vuejs.org/guide/component-provide-inject.html)方法。

### antdv关闭时销毁Modal里的子元素，设置destroyOnClose=true即可。

### 组件循环引用

- 异步导入（推荐）
```
components: { ComponentName: () => import('path for component') }
```
- 生命周期方法
```
beforeCreate () {
  this.$options.components.ComponentName = require('path for component').default
}
```

## 浏览器兼容

Modern browsers and IE10.

| [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/edge/edge_48x48.png" alt="IE / Edge" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>IE / Edge | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/firefox/firefox_48x48.png" alt="Firefox" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Firefox | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/chrome/chrome_48x48.png" alt="Chrome" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Chrome | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/safari/safari_48x48.png" alt="Safari" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Safari | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/opera/opera_48x48.png" alt="Opera" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Opera |
| --- | --- | --- | --- | --- |
| IE10, Edge | last 2 versions | last 2 versions | last 2 versions | last 2 versions |

## 参考

- [iconfont](https://www.iconfont.cn/)
- [vue-draggable](http://www.itxst.com/vue-draggable/tutorial.html)
