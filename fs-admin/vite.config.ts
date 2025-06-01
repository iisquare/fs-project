import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import copy from 'rollup-plugin-copy'

// https://vite.dev/config/
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks (id: string) {
          if (id.includes('/node_modules/')) {
            if (id.includes('lodash')) return 'lodash'
            if (id.includes('element')) return 'element'
            if (id.includes('vue') || id.includes('pinia')) return 'vue'
            return 'vender'
          } else if (id.includes('/src/')) {
            if (id.includes('/api/')) return 'api'
            if (id.includes('/views/')) return id.split('/views/')[1].split('/')[0]
          }
        }
      }
    },
    emptyOutDir: true,
    outDir: '../fs-java/web/admin/src/main/resources/static',
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
        additionalData: '@use "@/assets/mixin.scss" as *;',
      }
    },
  },
  plugins: [
    vue(),
    vueJsx(),
    vueDevTools(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
    copy({
      targets: [{ // 将首页拷贝至模板目录
        src: '../fs-java/web/admin/src/main/resources/static/index.html',
        dest: '../fs-java/web/admin/src/main/resources/templates/index/',
      }],
      hook: 'writeBundle', // 插件运行在rollup完成打包并将文件写入磁盘之前
      verbose: true
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
