import { resolve } from 'path'
import { defineConfig } from 'electron-vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import Icons from 'unplugin-icons/vite'
import IconsResolver from 'unplugin-icons/resolver'
import { FileSystemIconLoader } from 'unplugin-icons/loaders'

export default defineConfig({
  main: {
    build: {
      rollupOptions: {
        external: ['better-sqlite3-multiple-ciphers']
      }
    }
  },
  preload: {},
  renderer: {
    resolve: {
      alias: {
        '@renderer': resolve('src/renderer/src')
      }
    },
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router'],
        resolvers: [ElementPlusResolver()],
        dts: 'src/auto-imports.d.ts'
      }),
      Components({
        resolvers: [IconsResolver({ prefix: 'i' }), ElementPlusResolver()],
        dts: 'src/components.d.ts'
      }),
      Icons({
        customCollections: {
          plus: FileSystemIconLoader('src/renderer/src/assets/icons/plus'),
          layout: FileSystemIconLoader('src/renderer/src/assets/icons/layout'),
          ep: FileSystemIconLoader('src/renderer/src/assets/icons/ep')
        }
      })
    ]
  }
})
