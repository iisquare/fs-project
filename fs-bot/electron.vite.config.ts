import { createReadStream, cpSync, existsSync, statSync } from 'fs'
import { extname, join, resolve, sep } from 'path'
import { defineConfig } from 'electron-vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import Icons from 'unplugin-icons/vite'
import IconsResolver from 'unplugin-icons/resolver'
import { FileSystemIconLoader } from 'unplugin-icons/loaders'
import type { Plugin } from 'vite'

const VDITOR_ROOT = resolve('node_modules/vditor')
const VDITOR_MIME: Record<string, string> = {
  '.css': 'text/css; charset=utf-8',
  '.gif': 'image/gif',
  '.js': 'text/javascript; charset=utf-8',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2'
}

/**
 * Vditor 的 Lute、主题、高亮和图表运行时都由 cdn 参数动态加载。
 * 开发态从 node_modules 直接提供，构建时复制进 renderer 产物，保证离线可用且不违反 CSP。
 */
function vditorAssets(): Plugin {
  return {
    name: 'vditor-local-assets',
    configureServer(server) {
      server.middlewares.use((request, response, next) => {
        const pathname = String(request.url ?? '').split('?', 1)[0]
        if (!pathname.startsWith('/vditor/')) {
          next()
          return
        }
        let target = ''
        try {
          target = resolve(VDITOR_ROOT, `.${decodeURIComponent(pathname.slice('/vditor'.length))}`)
        } catch {
          response.statusCode = 400
          response.end()
          return
        }
        if (target !== VDITOR_ROOT && !target.startsWith(`${VDITOR_ROOT}${sep}`)) {
          response.statusCode = 403
          response.end()
          return
        }
        if (!existsSync(target) || !statSync(target).isFile()) {
          next()
          return
        }
        response.setHeader(
          'Content-Type',
          VDITOR_MIME[extname(target).toLowerCase()] ?? 'application/octet-stream'
        )
        response.setHeader('Cache-Control', 'no-cache')
        createReadStream(target).pipe(response)
      })
    },
    writeBundle(options) {
      const outDir = resolve(options.dir ?? 'out/renderer')
      cpSync(join(VDITOR_ROOT, 'dist'), join(outDir, 'vditor', 'dist'), { recursive: true })
    }
  }
}

export default defineConfig({
  main: {
    build: {
      rollupOptions: {
        // 桌面自动化依赖本地 .node 预编译模块，必须由 Electron 运行时直接 require。
        external: ['better-sqlite3-multiple-ciphers', '@nut-tree-fork/nut-js', 'koffi']
      }
    }
  },
  preload: {
    build: {
      rollupOptions: {
        input: {
          index: resolve('src/preload/index.ts'),
          'desktop-overlay': resolve('src/preload/desktop-overlay.ts')
        }
      }
    }
  },
  renderer: {
    resolve: {
      alias: {
        '@renderer': resolve('src/renderer/src')
      }
    },
    plugins: [
      vditorAssets(),
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
