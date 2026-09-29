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

/**
 * 取模块所属的包名。
 * pnpm 的路径形如 node_modules/.pnpm/<包名>@<版本>_<peer>/node_modules/<包名>/<文件>，
 * 必须从最后一个 node_modules 之后取，否则 peer 依赖名会被误判成本模块所属的包。
 */
function packageNameOf(id: string): string | undefined {
  const marker = '/node_modules/'
  const start = id.lastIndexOf(marker)
  if (start < 0) return undefined
  const [first, second] = id.slice(start + marker.length).split('/')
  if (!first || !second) return undefined
  return first.startsWith('@') ? `${first}/${second}` : first
}

/**
 * bpmn 设计器依赖家族（bpmn-js 及其独占依赖）。
 * 只有 OA 工作流设计器（designer-oa）用到，因此单独成 bpmn 分块随路由懒加载、不进入首屏。
 * 单拆的理由：bpmn-js 属于长期不变的第三方库，独立分块的 hash 不随业务代码改动而变化，
 * 改 Workflow/FlexForm 时回访用户只需重下 designer-oa（约 26 kB gzip），bpmn 那份继续命中缓存。
 * 必须整族放在同一分块，否则又会切出跨分块循环依赖。
 * 注意 min-dash/min-dom 等被 diagram-js 全族共用，故一并归入。
 * 未纳入的：nearley 族（sql-formatter 也用）、luxon（cron-parser 也用）、mousetrap（@antv/x6 也用）。
 */
const bpmnPackages = new Set([
  'bpmn-js',
  'bpmn-moddle',
  'diagram-js',
  'diagram-js-direct-editing',
  'component-event',
  'css.escape',
  'didi',
  'domify',
  'hammerjs',
  'ids',
  'inherits',
  'matches-selector',
  'min-dash',
  'min-dom',
  'moddle',
  'moddle-xml',
  'object-refs',
  'path-intersection',
  'saxen',
  'tiny-svg',
])

/**
 * 「仅按需使用」的重型依赖：只在个别页面或个别操作里用到，
 * 各自单列分块并由代码动态 import（见组件里的 ensureXxx），使它们不进入首屏。
 * 其中 nearley 族是 sql-formatter 的语法解析依赖、luxon 只被 cron-parser 使用，
 * 因此分别并入对应分块一起懒加载。
 */
const sqlFormatterPackages = new Set([
  'sql-formatter',
  'nearley',
  'moo',
  'randexp',
  'railroad-diagrams',
  'discontinuous-range',
])

/**
 * 第三方依赖分块：按「包名」精确归组。
 * 不能用 id.includes('element') 这类子串判断：diagram-js/lib/features/align-elements、
 * diagram-js/lib/core/ElementRegistry.js、zrender/lib/Element.js 等路径也含 "element"，
 * 会被误判成 element-plus 划入 element 分块，把 bpmn-js/diagram-js/zrender 拆到两个分块，
 * 形成跨分块循环依赖，运行时抛 "Cannot access 'xx' before initialization"。
 */
function vendorChunkName(pkg: string): string {
  if (pkg.startsWith('lodash')) return 'lodash'
  if (pkg === 'element-plus' || pkg.startsWith('@element-plus/')) return 'element'
  if (pkg === 'echarts' || pkg === 'zrender') return 'echarts'
  if (pkg === 'codemirror' || pkg.startsWith('@codemirror/')) return 'codemirror'
  if (pkg === 'cron-parser' || pkg === 'luxon') return 'cron-parser'
  if (sqlFormatterPackages.has(pkg)) return 'sql-formatter'
  if (pkg === 'vditor') return 'vditor'
  if (pkg.startsWith('@antv/')) return 'antv'
  if (bpmnPackages.has(pkg)) return 'bpmn'
  if (
    pkg === 'vue' ||
    pkg.startsWith('vue-') ||
    pkg === 'vuedraggable' ||
    pkg.startsWith('@vue/') ||
    pkg.startsWith('@vueuse/') ||
    pkg === 'pinia'
  ) return 'vue'
  return 'vender'
}

export default defineConfig({
  server: {
    allowedHosts: ['wsl'],
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks (id: string) {
          const pkg = packageNameOf(id)
          if (pkg) return vendorChunkName(pkg)
          if (id.includes('plugin-vue:export-helper')) return 'vue'
          /**
           * Vite 的预加载辅助模块（__vitePreload）被所有含动态 import 的分块共享。
           * 必须显式固定到 vender：否则 Rollup 会把它塞进某个懒分块的业务分块里，
           * 而所有用到动态 import 的分块（含入口）都要静态引入它，那个懒分块就会被首屏牵连进来。
           */
          if (id.includes('preload-helper')) return 'vender'
          if (id.includes('/src/')) { // 注意chunk合并规则，防止组件循环依赖
            if (id.includes('/api/') || id.includes('/core/') || id.includes('/stores/')) return 'api'
            /**
             * 「组件 + 专属大库」绑定到同一个分块：
             * 这些库只有个别组件用，把组件本身并进库分块后，用到的页面一次性把两者都取回，
             * 同目录里其他组件则不会被这些大库牵连（例如只用 DataFilter 的页面不该下 echarts）。
             */
            if (id.includes('/src/components/Data/DataResultChart.vue')) return 'echarts'
            if (id.includes('/src/components/Editor/CodeEditor.vue')) return 'codemirror'
            if (id.includes('/src/components/Editor/MarkdownEditor.vue')) return 'vditor'
            if (id.includes('/src/components/Form/FormCron.vue')) return 'cron-parser'
            /**
             * 通用组件按目录拆分。
             * 整个 src/components 合成一块时，只有个别页面用到的组件会被首屏一起带上，
             * 进而把它们静态依赖的 echarts / codemirror / vditor 等大库也拖进首屏。
             * 拆开后只有被首屏模块引用到的目录才进首屏，其余目录随使用它们的页面分块加载，
             * 即「打开页面一次性加载所需资源」，不需要在操作过程中再等待。
             * 若某两个目录互相引用并触发跨分块循环报错，把涉及的目录并回同一个分块即可。
             */
            if (id.includes('/src/components/')) return 'components-' + id.split('/src/components/')[1].split('/')[0].toLowerCase()
            // SqlUtil 依赖 sql-formatter（体积大，只有 BI 的 SQL 页面用），与库放同一分块，避免被 utils 带进首屏
            if (id.includes('/src/utils/SqlUtil.ts')) return 'sql-formatter'
            if (id.includes('/utils/')) return 'utils'
            if (id.includes('/views/')) return 'views-' + id.split('/views/')[1].split('/')[0]
            if (id.includes('/designer/')) {
              const name = id.split('/designer/')[1].split('/')[0]
              // 流程设计器需读取表单设计的字段权限，两者相互引用，合并为同一分块
              if (name === 'FlexForm' || name === 'Workflow') return 'designer-oa'
              return 'designer-' + name
            }
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
