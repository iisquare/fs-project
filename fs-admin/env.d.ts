/// <reference types="vite/client" />

declare const __VDITOR_VERSION__: string

declare module "*.vue" {
    import type { DefineComponent } from "vue"
    const vueComponent: DefineComponent<{}, {}, any>
    export default vueComponent
}

declare module 'element-plus/dist/locale/zh-cn.mjs'
