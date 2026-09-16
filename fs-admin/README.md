# fs-admin

This template should help get you started developing with Vue 3 in Vite.

## 如何运行

### 依赖版本

```
nvm: 1.1.12
node: v20.18.3
npm: 10.8.2
pnpm: 10.4.0
```

### 安装运行

- 安装
```sh
pnpm install
```

- 运行
```sh
pnpm run dev
# 已默认增加--host参数，开放所有地址访问
```

- 编译

```sh
pnpm build
```

## 目录约定

| 目录 | 用途 |
| --- | --- |
| `src/components/` | 公共组件，跨模块复用（Button、Form、Table、Dictionary、Layout、Chat、Editor 等封装） |
| `src/designer/` | 设计器组件，可视化编辑器（X6、FlexForm、TaskFlow、KnowledgeGraph、Agentic、Spider、Workflow） |
| `src/views/模块/components/` | 模块组件，仅在对应模块内使用（如 `src/views/kg/components/`、`src/views/oa/components/`、`src/views/bi/components/`） |

- 路由页面保存在 `src/views/模块/子模块/页面.vue`，非路由页面的组件统一放入对应模块的 `src/views/模块/components/` 目录。
- **除 `src/components/`、`src/designer/`、`src/views/模块/components/` 外，`src/views/` 下的文件必须都能被 `src/router/` 中的路由指向**，不允许存在非路由指向的文件。
- `components` 目录名保持小写；其下的子目录与 `.vue` 组件文件首字母大写（如 `KnowledgeImageEditor.vue`、`Chunk.vue`），非组件文件不受此限制（如 `config.ts`）。路由页面文件名保持小写，与路由中的引用一致（如 `list.vue`、`model.vue`）。
- 组件被两个及以上模块共用时，应移动到 `src/components/`。

## 参考文档

- [Vue3](https://cn.vuejs.org/guide/introduction.html)
- [Pinia](https://pinia.vuejs.org/zh/introduction.html)
- [Vue Router](https://router.vuejs.org/zh/introduction.html)
- [Element Plus](https://element-plus.org/zh-CN/component/overview.html)

- [CodeMirror](https://codemirror.net/)
- [vue-draggable](https://github.com/SortableJS/vue.draggable.next)
- [Antv X6](https://x6.antv.antgroup.com/tutorial/getting-started)
