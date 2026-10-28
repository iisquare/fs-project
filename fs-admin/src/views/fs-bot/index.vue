<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  ArrowRight,
  ChatDotRound,
  Download,
  FolderOpened,
  Lock,
  MagicStick,
  Share
} from '@element-plus/icons-vue'
import ClientReleaseApi from '@/api/agent/ClientReleaseApi'
import ApiUtil from '@/utils/ApiUtil'

interface ReleaseArtifact {
  id: number
  platform: string
  architecture: string
  channel: string
  version: string
  minimumVersion: string
  releaseNotes: string
  fileName: string
  fileSize: number
  downloadUrl: string
  createdTime: number
}

interface ReleaseGroup {
  version: string
  createdTime: number
  releaseNotes: string
  platforms: string[]
}

const releases = ref<ReleaseArtifact[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const loadError = ref('')
const loadMoreError = ref('')
const historyUnavailable = ref(false)
const showAllReleases = ref(false)
const releaseTotal = ref(0)
const activeSection = ref('')
let sectionObserver: IntersectionObserver | null = null

const RELEASE_PAGE_SIZE = 40

const platforms = [
  {
    key: 'win32',
    name: 'Windows',
    note: 'Windows 10 / 11',
    mark: 'win'
  },
  {
    key: 'darwin',
    name: 'macOS',
    note: 'macOS Intel / Apple 芯片',
    mark: 'mac'
  },
  {
    key: 'linux',
    name: 'Linux',
    note: 'AppImage 通用格式',
    mark: 'linux'
  }
]

const detectedPlatform = detectPlatform()
const requestedChannel = 'stable'

const sortedReleases = computed(() =>
  [...releases.value].sort((left, right) => compareVersion(right.version, left.version))
)

const latestVersion = computed(() => sortedReleases.value[0]?.version || '')

const releaseGroups = computed<ReleaseGroup[]>(() => {
  const groups = new Map<string, ReleaseGroup>()
  for (const item of sortedReleases.value) {
    let group = groups.get(item.version)
    if (!group) {
      group = {
        version: item.version,
        createdTime: item.createdTime,
        releaseNotes: item.releaseNotes,
        platforms: []
      }
      groups.set(item.version, group)
    }
    group.createdTime = Math.max(group.createdTime, item.createdTime || 0)
    if (!group.releaseNotes && item.releaseNotes) group.releaseNotes = item.releaseNotes
    if (!group.platforms.includes(item.platform)) group.platforms.push(item.platform)
  }
  return [...groups.values()].sort((left, right) => compareVersion(right.version, left.version))
})

const visibleReleases = computed(() =>
  showAllReleases.value ? releaseGroups.value : releaseGroups.value.slice(0, 6)
)

const hasMoreReleases = computed(
  () => releases.value.length < releaseTotal.value && !loadingMore.value
)

function detectPlatform(): string {
  const agent = navigator.userAgent.toLowerCase()
  if (agent.includes('windows') || agent.includes('win32')) return 'win32'
  if (agent.includes('mac os') || agent.includes('macintosh')) return 'darwin'
  if (agent.includes('linux')) return 'linux'
  return ''
}

function platformName(key: string): string {
  return platforms.find((item) => item.key === key)?.name || key
}

function architectureName(value: string): string {
  const labels: Record<string, string> = {
    x64: 'x64',
    arm64: 'ARM64',
    ia32: 'x86',
    armv7l: 'ARM',
    universal: 'Universal'
  }
  return labels[value] || value
}

function latestOf(platform: string): ReleaseArtifact[] {
  const candidates = sortedReleases.value.filter((item) => item.platform === platform)
  const version = candidates[0]?.version
  if (!version) return []
  return candidates
    .filter((item) => item.version === version)
    .sort(
      (left, right) =>
        architectureRank(left.architecture) - architectureRank(right.architecture)
    )
}

function latestVersionOf(platform: string): string {
  return latestOf(platform)[0]?.version || ''
}

function architectureRank(value: string): number {
  return ['x64', 'universal', 'arm64', 'ia32', 'armv7l'].indexOf(value)
}

function recommendedArtifact(): ReleaseArtifact | undefined {
  return latestOf(detectedPlatform)[0] || sortedReleases.value[0]
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '大小未知'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  return `${value.toFixed(unit > 1 ? 1 : 0)} ${units[unit]}`
}

function formatDate(value: number): string {
  if (!value) return '日期未知'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(value))
}

function compareVersion(left: string, right: string): number {
  const partsOf = (value: string) => {
    const source = String(value || '').replace(/^v/i, '').split('+')[0]
    const [core, pre = ''] = source.split('-', 2)
    return {
      core: core.split('.').map((item) => item.replace(/^0+(?=\d)/, '')),
      pre: pre ? pre.split('.') : []
    }
  }
  const a = partsOf(left)
  const b = partsOf(right)
  const length = Math.max(a.core.length, b.core.length)
  for (let index = 0; index < length; index += 1) {
    const x = a.core[index] || '0'
    const y = b.core[index] || '0'
    if (x.length !== y.length) return x.length - y.length
    if (x !== y) return x.localeCompare(y)
  }
  if (!a.pre.length && !b.pre.length) return 0
  if (!a.pre.length) return 1
  if (!b.pre.length) return -1
  const preLength = Math.max(a.pre.length, b.pre.length)
  for (let index = 0; index < preLength; index += 1) {
    if (a.pre[index] === undefined) return -1
    if (b.pre[index] === undefined) return 1
    const x = a.pre[index]
    const y = b.pre[index]
    const xNumber = /^\d+$/.test(x)
    const yNumber = /^\d+$/.test(y)
    if (xNumber && yNumber && x !== y) return Number(x) - Number(y)
    if (xNumber !== yNumber) return xNumber ? -1 : 1
    if (x !== y) return x.localeCompare(y)
  }
  return 0
}

function absoluteApiUrl(path: string): string {
  const value = String(path || '')
  if (/^https?:\/\//i.test(value)) return value
  const base = String(import.meta.env.VITE_APP_API_URL || '').replace(/\/+$/, '')
  return base + (value.startsWith('/') ? value : `/${value}`)
}

function download(item?: ReleaseArtifact): void {
  if (!item?.downloadUrl) return
  window.open(absoluteApiUrl(item.downloadUrl), '_blank', 'noopener,noreferrer')
}

/**
 * 页面路由本身就在 URL 的 hash 中，不能直接用 href="#features"：
 * 浏览器会把 #/fs-bot 覆盖成 #features，随后被路由识别成 /features。
 */
function scrollToSection(id: string): void {
  if (id === 'top') {
    window.scrollTo({
      top: 0,
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
        ? 'auto'
        : 'smooth'
    })
    activeSection.value = ''
    return
  }
  const target = document.getElementById(id)
  if (!target) return
  target.scrollIntoView({
    block: 'start',
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
      ? 'auto'
      : 'smooth'
  })
}

async function loadReleases(): Promise<void> {
  loading.value = true
  loadError.value = ''
  loadMoreError.value = ''
  historyUnavailable.value = false
  try {
    const result = await ClientReleaseApi.releases(
      { channel: requestedChannel, offset: 0, limit: RELEASE_PAGE_SIZE },
      { warning: false, error: false }
    )
    const data = ApiUtil.data(result) || {}
    releases.value = Array.isArray(data.rows) ? data.rows : []
    releaseTotal.value = Number(data.total ?? releases.value.length)
    if (!releases.value.length) {
      await loadFallback()
    }
  } catch {
    try {
      await loadFallback()
      historyUnavailable.value = true
    } catch {
      loadError.value = '暂时无法读取发布信息，请稍后刷新页面。'
    }
  } finally {
    loading.value = false
  }
}

async function loadMoreReleases(): Promise<void> {
  if (loadingMore.value || !hasMoreReleases.value) return
  loadingMore.value = true
  loadMoreError.value = ''
  try {
    const result = await ClientReleaseApi.releases(
      {
        channel: requestedChannel,
        offset: releases.value.length,
        limit: RELEASE_PAGE_SIZE
      },
      { warning: false, error: false }
    )
    const data = ApiUtil.data(result) || {}
    const rows: ReleaseArtifact[] = Array.isArray(data.rows) ? data.rows : []
    const known = new Set(releases.value.map((item) => item.id))
    const additions = rows.filter((item) => !known.has(item.id))
    releases.value.push(...additions)
    releaseTotal.value = Number(data.total ?? releases.value.length)
    // 兼容尚未支持 offset 的旧服务端，避免一直请求到同一页。
    if (!additions.length) releaseTotal.value = releases.value.length
  } catch {
    loadMoreError.value = '更早的发布日志加载失败，请稍后重试。'
  } finally {
    loadingMore.value = false
  }
}

/** 新接口尚未部署时，仍通过已有的公开检查接口展示各系统最新版。 */
async function loadFallback(): Promise<void> {
  const checks = await Promise.allSettled(
    platforms.map((platform) =>
      ClientReleaseApi.check(
        {
          currentVersion: '0.0.0',
          platform: platform.key,
          architecture: 'x64',
          clientId: 'fs-bot-public-release-page',
          channel: requestedChannel
        },
        { warning: false, error: false }
      )
    )
  )
  const rows: ReleaseArtifact[] = []
  for (const check of checks) {
    if (check.status !== 'fulfilled') continue
    const latest = ApiUtil.data(check.value)?.latest
    if (latest?.downloadUrl) rows.push(latest as ReleaseArtifact)
  }
  if (!rows.length) throw new Error('no public release')
  releases.value = rows
  releaseTotal.value = rows.length
}

onMounted(() => {
  document.getElementById('app')?.classList.add('fs-bot-public')
  void loadReleases()
  const sections = ['features', 'download', 'releases']
    .map((id) => document.getElementById(id))
    .filter((element): element is HTMLElement => element !== null)
  sectionObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0]
      if (visible) activeSection.value = visible.target.id
    },
    { rootMargin: '-24% 0px -60% 0px', threshold: [0, 0.2, 0.5] }
  )
  sections.forEach((section) => sectionObserver?.observe(section))
})

onBeforeUnmount(() => {
  document.getElementById('app')?.classList.remove('fs-bot-public')
  sectionObserver?.disconnect()
  sectionObserver = null
})
</script>

<template>
  <div class="bot-site">
    <header class="site-nav">
      <a class="site-brand" href="#top" aria-label="返回页面顶部" @click.prevent="scrollToSection('top')">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 48 48">
            <circle cx="24" cy="24" r="3.3" />
            <ellipse cx="24" cy="24" rx="8.2" ry="17.5" transform="rotate(48 24 24)" />
            <ellipse cx="24" cy="24" rx="8.2" ry="17.5" transform="rotate(-48 24 24)" />
            <circle class="brand-node" cx="38.2" cy="34.3" r="2.4" />
            <circle class="brand-node" cx="9.8" cy="34.3" r="2.4" />
          </svg>
        </span>
        <span class="brand-copy">
          <strong>fs-bot</strong>
          <small>智能体工作台</small>
        </span>
      </a>
      <nav class="nav-links" aria-label="页面导航">
        <a
          href="#features"
          :class="{ 'is-active': activeSection === 'features' }"
          @click.prevent="scrollToSection('features')"
        >
          功能
        </a>
        <a
          href="#download"
          :class="{ 'is-active': activeSection === 'download' }"
          @click.prevent="scrollToSection('download')"
        >
          下载
        </a>
        <a
          href="#releases"
          :class="{ 'is-active': activeSection === 'releases' }"
          @click.prevent="scrollToSection('releases')"
        >
          发布日志
        </a>
      </nav>
      <router-link class="nav-console" to="/">
        控制台
        <el-icon><ArrowRight /></el-icon>
      </router-link>
    </header>

    <main id="top">
      <section class="hero">
        <div class="hero-glow hero-glow--one"></div>
        <div class="hero-glow hero-glow--two"></div>
        <div class="hero-copy">
          <div class="eyebrow"><span></span> FS-BOT / DESKTOP AGENT</div>
          <h1>
            <span>把模型、知识与工具，</span>
            <em>装进一张工作台。</em>
          </h1>
          <p class="hero-lead">
            面向真实工作流的桌面智能体。项目、对话、技能与本地工作区在一个地方协作，
            让模型不止回答问题，也能沿着你的上下文把事情做完。
          </p>
          <div class="hero-actions">
            <button
              class="primary-action"
              type="button"
              :disabled="loading || !recommendedArtifact()"
              @click="download(recommendedArtifact())"
            >
              <el-icon><Download /></el-icon>
              {{
                loading
                  ? '正在读取版本'
                  : detectedPlatform
                    ? `下载 ${platformName(detectedPlatform)} 版`
                    : '下载最新版'
              }}
            </button>
            <a class="secondary-action" href="#features" @click.prevent="scrollToSection('features')">
              看看它能做什么
              <el-icon><ArrowRight /></el-icon>
            </a>
          </div>
          <div class="hero-meta">
            <span class="live-dot"></span>
            <template v-if="latestVersion">
              稳定版 <b>v{{ latestVersion }}</b>
              <i></i>
              支持 Windows / macOS / Linux
            </template>
            <template v-else>{{ loading ? '正在获取最新发布信息' : '发布信息暂不可用' }}</template>
          </div>
        </div>

        <div class="hero-visual" aria-label="fs-bot 工作台界面示意">
          <div class="orbit orbit--outer"></div>
          <div class="orbit orbit--inner"></div>
          <div class="product-window">
            <div class="window-bar">
              <div class="window-dots"><span></span><span></span><span></span></div>
              <div class="window-title">知识库助手 · 产品资料整理</div>
              <div class="window-badge">FS</div>
            </div>
            <div class="window-body">
              <aside class="mock-sidebar">
                <div class="mock-brand">
                  <span class="mock-mark">F</span>
                  <span>工作区</span>
                </div>
                <div class="mock-new">＋ 新建对话</div>
                <div class="mock-label">最近</div>
                <div class="mock-chat is-active"><span></span>产品资料整理</div>
                <div class="mock-chat"><span></span>竞品功能对照</div>
                <div class="mock-chat"><span></span>发布说明草稿</div>
                <div class="mock-spacer"></div>
                <div class="mock-project">
                  <small>当前项目</small>
                  <strong>FS 产品工作台</strong>
                  <span>8 个工具 · 3 个技能</span>
                </div>
              </aside>
              <div class="mock-main">
                <div class="mock-header">
                  <div><small>PROJECT</small><strong>FS 产品工作台</strong></div>
                  <span>•••</span>
                </div>
                <div class="mock-thread">
                  <div class="mock-user">梳理最近三个版本的变化，并整理成发布说明。</div>
                  <div class="mock-thinking">
                    <span class="mock-avatar">F</span>
                    <div>
                      <small>FS-BOT</small>
                      <p>我会先读取版本记录，再按用户影响归类。</p>
                    </div>
                  </div>
                  <div class="mock-tools">
                    <span><i></i> 已读取 3 份文档</span>
                    <span><i></i> 已生成发布草稿</span>
                  </div>
                  <div class="mock-answer">
                    <div class="mock-line mock-line--wide"></div>
                    <div class="mock-line"></div>
                    <div class="mock-line mock-line--short"></div>
                    <div class="mock-file">
                      <span class="file-icon">MD</span>
                      <div><strong>release-notes.md</strong><small>已创建 · 本地工作区</small></div>
                      <span class="file-check">✓</span>
                    </div>
                  </div>
                </div>
                <div class="mock-composer">
                  <span>继续提问，或输入 / 使用技能</span>
                  <b>↑</b>
                </div>
              </div>
            </div>
          </div>
          <div class="floating-card floating-card--tools">
            <span class="floating-icon"><MagicStick /></span>
            <div><strong>技能已就绪</strong><small>按需加载，不在对话里走神</small></div>
          </div>
          <div class="floating-card floating-card--local">
            <span class="floating-pulse"></span>
            <div><strong>本地执行</strong><small>访问范围始终由你决定</small></div>
          </div>
        </div>
      </section>

      <section class="proof-strip" aria-label="产品特点">
        <div><strong>多模型</strong><span>按项目选择合适的能力</span></div>
        <div><strong>本地优先</strong><span>项目与会话保存在身边</span></div>
        <div><strong>可组合</strong><span>技能、知识与工具自由编排</span></div>
        <div><strong>跨平台</strong><span>Windows、macOS 与 Linux</span></div>
      </section>

      <section id="features" class="section features">
        <div class="section-heading">
          <div>
            <span class="section-kicker">ONE DESK, WHOLE CONTEXT</span>
            <h2>不只是一轮对话，<br />而是一套能持续工作的上下文。</h2>
          </div>
          <p>
            从项目配置到工具调用，每个环节都围绕同一个目标展开。历史、记忆、工作区和交付物彼此连接，
            下一轮不必重新解释一遍。
          </p>
        </div>

        <div class="feature-grid">
          <article class="feature-card feature-card--hero">
            <div class="feature-icon"><ChatDotRound /></div>
            <div>
              <span class="feature-index">01</span>
              <h3>有上下文的深度对话</h3>
              <p>
                按项目组织系统提示词、长期记忆与上下文窗口。对话支持分支、编辑重发和生成队列，
                思路走得再远，也能回到原来的那条路径。
              </p>
            </div>
            <div class="branch-demo" aria-hidden="true">
              <span class="branch-node">原始提问</span>
              <span class="branch-node branch-node--active">当前分支</span>
              <span class="branch-node">另一个方向</span>
              <svg viewBox="0 0 320 70">
                <path d="M42 20 C90 20 82 35 130 35 S185 35 204 35" />
                <path d="M204 35 C235 35 240 58 280 58" />
              </svg>
            </div>
          </article>

          <article class="feature-card">
            <div class="feature-icon"><FolderOpened /></div>
            <span class="feature-index">02</span>
            <h3>看得见、管得住的工作区</h3>
            <p>
              模型读写文件、执行命令都要经过项目目录权限。工作区之外可以逐次确认，
              重要动作留在你的视野里。
            </p>
            <div class="permission-demo">
              <span>写入 release-notes.md</span>
              <b>允许本次</b>
            </div>
          </article>

          <article class="feature-card">
            <div class="feature-icon"><MagicStick /></div>
            <span class="feature-index">03</span>
            <h3>技能与工具按需加入</h3>
            <p>
              从服务端安装团队技能，把知识库、数据与业务能力作为工具带进项目。
              每项能力都有清晰说明，需要时再调用。
            </p>
            <div class="tool-cloud">
              <span>知识检索</span><span>文档生成</span><span>数据分析</span>
            </div>
          </article>

          <article class="feature-card feature-card--quiet">
            <div class="feature-icon"><Share /></div>
            <span class="feature-index">04</span>
            <h3>项目可以流动起来</h3>
            <p>
              导出项目配置、通过版本记录分享工作方式，也可以从团队其他人的项目开始，
              保留方法，不必从空白页重来。
            </p>
          </article>
        </div>
      </section>

      <section id="download" class="section download-section">
        <div class="download-heading">
          <div>
            <span class="section-kicker">DOWNLOAD FOR YOUR DESK</span>
            <h2>选择你的系统，<br />从这里开始。</h2>
          </div>
          <div class="channel-note">
            <span class="live-dot"></span>
            <div>
              <strong>{{ latestVersion ? `当前稳定版 v${latestVersion}` : '正在读取稳定版' }}</strong>
              <small>安装包由服务端版本记录实时提供</small>
            </div>
          </div>
        </div>

        <div v-if="loadError" class="load-alert">
          <Lock />
          <span>{{ loadError }}</span>
          <button type="button" @click="loadReleases">重新加载</button>
        </div>

        <div class="download-grid">
          <article
            v-for="platform in platforms"
            :key="platform.key"
            class="download-card"
            :class="{
              'is-recommended': detectedPlatform === platform.key,
              'is-unavailable': !loading && !latestOf(platform.key).length
            }"
          >
            <div class="download-card-top">
              <span class="platform-mark" :class="`platform-mark--${platform.mark}`">
                <template v-if="platform.key === 'win32'">
                  <i></i><i></i><i></i><i></i>
                </template>
                <template v-else-if="platform.key === 'darwin'">⌘</template>
                <template v-else>&gt;_</template>
              </span>
              <span v-if="detectedPlatform === platform.key" class="recommended-tag">你的系统</span>
            </div>
            <h3>{{ platform.name }}</h3>
            <p>{{ platform.note }}</p>
            <div class="download-version">
              <template v-if="latestVersionOf(platform.key)">
                <strong>v{{ latestVersionOf(platform.key) }}</strong>
                <span>{{ formatFileSize(latestOf(platform.key)[0]?.fileSize || 0) }}</span>
              </template>
              <template v-else>
                <strong>{{ loading ? '读取中…' : '暂无公开版本' }}</strong>
              </template>
            </div>
            <div class="download-actions">
              <button
                v-for="item in latestOf(platform.key)"
                :key="item.id"
                type="button"
                class="platform-download"
                @click="download(item)"
              >
                <el-icon><Download /></el-icon>
                {{ architectureName(item.architecture) }}
              </button>
              <span v-if="!loading && !latestOf(platform.key).length" class="download-empty">
                稍后再来看看
              </span>
            </div>
          </article>
        </div>
        <p class="download-footnote">
          Windows 使用 NSIS 安装包，macOS 使用 DMG，Linux 使用 AppImage。
          如需批量部署或测试渠道，请联系管理员。
        </p>
      </section>

      <section id="releases" class="section releases-section">
        <div class="release-heading">
          <div>
            <span class="section-kicker">RELEASE NOTES</span>
            <h2>每一次变化，<br />都有迹可循。</h2>
          </div>
          <span v-if="latestVersion" class="latest-pill">LATEST · v{{ latestVersion }}</span>
        </div>

        <div v-if="historyUnavailable" class="history-note">
          版本列表接口暂不可用，当前仅展示各系统最新版。历史日志会在服务更新后自动出现。
        </div>

        <div v-if="!loading && !releaseGroups.length" class="release-empty">
          <span>还没有公开的发布记录。</span>
          <small>新版本完成发布后，会在这里同步更新。</small>
        </div>

        <div v-else class="release-list" :class="{ 'is-loading': loading }">
          <details
            v-for="(item, index) in visibleReleases"
            :key="item.version"
            class="release-item"
            :open="index === 0"
          >
            <summary>
              <span class="release-date">{{ formatDate(item.createdTime) }}</span>
              <span class="release-version">v{{ item.version }}</span>
              <span v-if="index === 0" class="release-latest">最新</span>
              <span class="release-platforms">
                <i v-for="platform in item.platforms" :key="platform">
                  {{ platformName(platform) }}
                </i>
              </span>
              <span class="release-toggle" aria-hidden="true">
                <svg viewBox="0 0 16 16">
                  <path d="m4 6 4 4 4-4" />
                </svg>
              </span>
            </summary>
            <div class="release-content">
              <p>{{ item.releaseNotes || '本次发布暂无详细说明。' }}</p>
            </div>
          </details>
          <div v-if="loading" class="release-skeletons" aria-label="正在加载发布日志">
            <span></span><span></span><span></span>
          </div>
        </div>

        <div
          v-if="releaseGroups.length > 6 || hasMoreReleases || loadMoreError"
          class="release-actions"
        >
          <button
            v-if="releaseGroups.length > 6"
            type="button"
            class="release-action"
            @click="showAllReleases = !showAllReleases"
          >
            {{
              showAllReleases
                ? '收起发布日志'
                : `展开已加载的 ${releaseGroups.length} 个版本`
            }}
            <el-icon :class="{ 'is-up': showAllReleases }"><ArrowRight /></el-icon>
          </button>
          <button
            v-if="hasMoreReleases || loadingMore"
            type="button"
            class="release-action release-action--primary"
            :disabled="loadingMore"
            @click="loadMoreReleases"
          >
            {{ loadingMore ? '正在加载…' : '加载更早版本' }}
          </button>
          <span v-if="releaseTotal" class="release-range">
            已加载 {{ releases.length }} / {{ releaseTotal }} 条发布记录
          </span>
        </div>
        <p v-if="loadMoreError" class="release-load-error">
          {{ loadMoreError }}
          <button type="button" @click="loadMoreReleases">重试</button>
        </p>
      </section>

      <section class="closing">
        <div class="closing-orbit" aria-hidden="true"></div>
        <span class="section-kicker">READY WHEN YOU ARE</span>
        <h2>让下一次工作，<br />从理解你的上下文开始。</h2>
        <p>下载 fs-bot，把团队积累的模型、知识与工具带到桌面上。</p>
        <button
          class="primary-action primary-action--light"
          type="button"
          :disabled="loading || !recommendedArtifact()"
          @click="download(recommendedArtifact())"
        >
          <el-icon><Download /></el-icon>
          下载最新版
        </button>
      </section>
    </main>

    <footer class="site-footer">
      <div class="site-brand site-brand--footer">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 48 48">
            <circle cx="24" cy="24" r="3.3" />
            <ellipse cx="24" cy="24" rx="8.2" ry="17.5" transform="rotate(48 24 24)" />
            <ellipse cx="24" cy="24" rx="8.2" ry="17.5" transform="rotate(-48 24 24)" />
            <circle class="brand-node" cx="38.2" cy="34.3" r="2.4" />
            <circle class="brand-node" cx="9.8" cy="34.3" r="2.4" />
          </svg>
        </span>
        <span class="brand-copy"><strong>fs-bot</strong><small>智能体工作台</small></span>
      </div>
      <p>Built for focused work.</p>
      <div class="footer-links">
        <a href="#download" @click.prevent="scrollToSection('download')">下载</a>
        <a href="#releases" @click.prevent="scrollToSection('releases')">发布日志</a>
        <router-link to="/">控制台</router-link>
      </div>
    </footer>
  </div>
</template>

<style lang="scss" scoped>
$night: #101722;
$night-soft: #172231;
$ink: #17212d;
$muted: #657181;
$line: #dde3e7;
$paper: #f5f7f6;
$white: #ffffff;
$cyan: #82dbe7;
$cyan-deep: #45b9ca;
$lime: #dff29a;

.bot-site {
  --site-width: 1200px;
  width: 100%;
  max-width: 100vw;
  min-width: 320px;
  min-height: 100vh;
  overflow-x: clip;
  color: $ink;
  background: $paper;
  font-family: Inter, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

:global(#app.fs-bot-public) {
  min-width: 0;
  overflow-x: hidden;
}

.site-nav {
  position: sticky;
  z-index: 30;
  top: 0;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  width: 100%;
  height: 76px;
  padding: 0 max(32px, calc((100% - var(--site-width)) / 2));
  background: rgb(245 247 246 / 84%);
  border-bottom: 1px solid rgb(37 52 64 / 7%);
  backdrop-filter: blur(18px) saturate(135%);
}

.site-brand {
  display: inline-flex;
  align-items: center;
  justify-self: start;
  gap: 11px;
  color: $ink;
}

.brand-mark {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  background: $night;
  border-radius: 11px;
  box-shadow: 0 8px 20px rgb(16 23 34 / 18%);

  svg {
    width: 32px;
    height: 32px;
    fill: none;
    stroke: $cyan;
    stroke-width: 1.8;
  }

  circle:first-child {
    fill: $cyan;
    stroke: none;
  }

  .brand-node {
    fill: $night;
    stroke: $cyan;
  }
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;

  strong {
    font-size: 17px;
    line-height: 1.2;
    letter-spacing: -0.02em;
  }

  small {
    color: $muted;
    font-size: 10px;
    letter-spacing: 0.08em;
  }
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 34px;

  a {
    position: relative;
    padding: 8px 0;
    color: #596473;
    font-size: 13px;
    white-space: nowrap;
    transition: color 0.2s;

    &::after {
      position: absolute;
      right: 0;
      bottom: 2px;
      left: 0;
      height: 1px;
      content: '';
      background: $ink;
      transform: scaleX(0);
      transform-origin: right;
      transition: transform 0.25s;
    }

    &:hover {
      color: $ink;

      &::after {
        transform: scaleX(1);
        transform-origin: left;
      }
    }

    &.is-active {
      color: $ink;
      font-weight: 600;

      &::after {
        background: $cyan-deep;
        transform: scaleX(1);
        transform-origin: left;
      }
    }
  }
}

.nav-console {
  display: inline-flex;
  align-items: center;
  justify-self: end;
  gap: 7px;
  padding: 10px 15px;
  color: $ink;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #d8dfe2;
  border-radius: 999px;
  background: rgb(255 255 255 / 58%);
  transition:
    border-color 0.2s,
    background 0.2s,
    transform 0.2s;

  &:hover {
    border-color: #aeb9be;
    background: $white;
    transform: translateY(-1px);
  }
}

.site-brand:focus-visible,
.nav-links a:focus-visible,
.nav-console:focus-visible,
.primary-action:focus-visible,
.secondary-action:focus-visible,
.platform-download:focus-visible,
.release-action:focus-visible {
  outline: 2px solid $cyan-deep;
  outline-offset: 3px;
}

.site-nav a {
  text-decoration: none;
}

.hero,
.proof-strip,
.section,
.site-footer {
  scroll-margin-top: 92px;
}

.hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(520px, 0.95fr);
  align-items: center;
  gap: 52px;
  width: min(calc(100% - 64px), var(--site-width));
  min-height: 660px;
  margin: 0 auto;
  padding: 52px 0 82px;
}

.hero::before {
  position: absolute;
  z-index: 0;
  top: -110px;
  right: -40vw;
  width: 95vw;
  height: 760px;
  content: '';
  opacity: 0.45;
  background-image: radial-gradient(#bfc8cb 0.7px, transparent 0.7px);
  background-size: 22px 22px;
  mask-image: radial-gradient(circle at 50% 50%, #000 0%, transparent 72%);
}

.hero-glow {
  position: absolute;
  z-index: 0;
  border-radius: 50%;
  filter: blur(12px);
  pointer-events: none;

  &--one {
    top: 40px;
    right: -120px;
    width: 420px;
    height: 420px;
    background: rgb(130 219 231 / 18%);
  }

  &--two {
    right: 290px;
    bottom: 10px;
    width: 220px;
    height: 220px;
    background: rgb(223 242 154 / 20%);
  }
}

.hero-copy,
.hero-visual {
  position: relative;
  z-index: 2;
}

.eyebrow,
.section-kicker {
  color: #73808c;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 24px;

  span {
    width: 23px;
    height: 1px;
    background: $cyan-deep;
  }
}

.hero h1 {
  max-width: 640px;
  font-size: clamp(44px, 4.8vw, 60px);
  font-weight: 520;
  line-height: 1.08;
  letter-spacing: -0.055em;

  > span {
    display: block;
    white-space: nowrap;
  }

  em {
    display: block;
    color: #64717f;
    font-family: 'Songti SC', 'STSong', serif;
    font-style: normal;
    font-weight: 400;
  }
}

.hero-lead {
  max-width: 520px;
  margin-top: 24px;
  color: $muted;
  font-size: 15px;
  line-height: 1.85;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 22px;
  margin-top: 30px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 48px;
  padding: 0 20px;
  font: inherit;
  font-size: 14px;
  font-weight: 650;
  border: 0;
  cursor: pointer;
}

.primary-action {
  color: #eafbfd;
  background: $night;
  border-radius: 11px;
  box-shadow: 0 12px 28px rgb(16 23 34 / 18%);
  transition:
    transform 0.2s,
    box-shadow 0.2s;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 16px 34px rgb(16 23 34 / 24%);
  }

  &:disabled {
    cursor: wait;
    opacity: 0.65;
  }
}

.secondary-action {
  padding: 0;
  color: $ink;
  background: transparent;

  .el-icon {
    transition: transform 0.2s;
  }

  &:hover .el-icon {
    transform: translateX(4px);
  }
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 25px;
  color: #7d8792;
  font-size: 11px;

  b {
    color: #4b5764;
    font-weight: 650;
  }

  i {
    width: 1px;
    height: 10px;
    margin: 0 4px;
    background: #ccd3d6;
  }
}

.live-dot {
  position: relative;
  width: 7px;
  height: 7px;
  flex: none;
  background: #70b98a;
  border-radius: 50%;

  &::after {
    position: absolute;
    inset: -4px;
    content: '';
    border: 1px solid rgb(112 185 138 / 28%);
    border-radius: 50%;
  }
}

.hero-visual {
  min-height: 530px;
  isolation: isolate;
}

.orbit {
  position: absolute;
  border: 1px solid rgb(90 105 116 / 11%);
  border-radius: 50%;
  pointer-events: none;

  &--outer {
    top: 2%;
    left: 1%;
    width: 570px;
    height: 570px;
  }

  &--inner {
    top: 13%;
    left: 17%;
    width: 430px;
    height: 430px;
    border-style: dashed;
  }
}

.product-window {
  position: absolute;
  top: 22px;
  left: 50%;
  width: 620px;
  overflow: hidden;
  background: #fbfcfc;
  border: 1px solid rgb(31 45 57 / 13%);
  border-radius: 17px;
  box-shadow:
    0 44px 90px rgb(30 44 57 / 17%),
    0 8px 24px rgb(30 44 57 / 8%);
  transform: translateX(-50%) rotate(-1.2deg);
  animation: window-enter 0.9s cubic-bezier(0.2, 0.7, 0.2, 1) both;
}

@keyframes window-enter {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(22px) rotate(-2deg);
  }
}

.window-bar {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  height: 42px;
  padding: 0 14px;
  color: #6d7883;
  font-size: 10px;
  border-bottom: 1px solid #e7ebed;
  background: #f4f6f6;
}

.window-dots {
  display: flex;
  gap: 5px;

  span {
    width: 7px;
    height: 7px;
    background: #ccd3d7;
    border-radius: 50%;

    &:first-child {
      background: #efb2a9;
    }

    &:nth-child(2) {
      background: #ead69e;
    }

    &:nth-child(3) {
      background: #add3b2;
    }
  }
}

.window-title {
  font-weight: 600;
}

.window-badge {
  display: grid;
  justify-self: end;
  width: 21px;
  height: 21px;
  color: $cyan;
  font-size: 9px;
  font-weight: 700;
  place-items: center;
  background: $night;
  border-radius: 6px;
}

.window-body {
  display: grid;
  grid-template-columns: 142px 1fr;
  height: 408px;
}

.mock-sidebar {
  display: flex;
  flex-direction: column;
  padding: 19px 11px 12px;
  color: #d3dae0;
  background: $night-soft;
}

.mock-brand {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 5px 16px;
  font-size: 10px;
  font-weight: 600;
}

.mock-mark {
  display: grid;
  width: 22px;
  height: 22px;
  color: $night;
  font-size: 9px;
  font-weight: 800;
  place-items: center;
  background: $cyan;
  border-radius: 6px;
}

.mock-new {
  margin-bottom: 17px;
  padding: 7px;
  color: #e7eef2;
  font-size: 9px;
  border: 1px solid #354252;
  border-radius: 6px;
  text-align: center;
}

.mock-label {
  margin: 0 6px 7px;
  color: #7d8a98;
  font-size: 8px;
  letter-spacing: 0.08em;
}

.mock-chat {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 8px;
  color: #b8c1ca;
  font-size: 8px;
  border-radius: 6px;

  span {
    width: 5px;
    height: 5px;
    background: #526171;
    border-radius: 50%;
  }

  &.is-active {
    color: #f0f5f7;
    background: #263546;

    span {
      background: $cyan;
    }
  }
}

.mock-spacer {
  flex: 1;
}

.mock-project {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px;
  background: #1d2a3a;
  border: 1px solid #2c3c4e;
  border-radius: 8px;

  small {
    color: #758493;
    font-size: 7px;
    letter-spacing: 0.08em;
  }

  strong {
    color: #e3eaee;
    font-size: 9px;
  }

  span {
    color: #7f8d9b;
    font-size: 7px;
  }
}

.mock-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fcfdfd;
}

.mock-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 54px;
  padding: 0 22px;
  border-bottom: 1px solid #edf0f1;

  div {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  small {
    color: #a0a9b0;
    font-size: 6px;
    letter-spacing: 0.14em;
  }

  strong {
    color: #3a4652;
    font-size: 10px;
  }

  > span {
    color: #929da6;
    font-size: 10px;
    letter-spacing: 0.15em;
  }
}

.mock-thread {
  flex: 1;
  padding: 22px 24px 10px;
}

.mock-user {
  width: 68%;
  margin: 0 0 20px auto;
  padding: 10px 12px;
  color: #46525e;
  font-size: 8px;
  line-height: 1.6;
  background: #eef2f3;
  border-radius: 9px 9px 2px 9px;
}

.mock-thinking {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
  color: #7c8791;

  > div {
    padding-top: 2px;
  }

  small {
    color: #40505e;
    font-size: 6px;
    font-weight: 700;
    letter-spacing: 0.1em;
  }

  p {
    margin-top: 4px;
    font-size: 8px;
    line-height: 1.6;
  }
}

.mock-avatar {
  display: grid;
  width: 22px;
  height: 22px;
  flex: none;
  color: $cyan;
  font-size: 8px;
  font-weight: 700;
  place-items: center;
  background: $night;
  border-radius: 7px;
}

.mock-tools {
  display: flex;
  gap: 6px;
  margin: 0 0 13px 30px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 5px 7px;
    color: #7b8892;
    font-size: 6px;
    background: #f4f7f7;
    border: 1px solid #e6ebec;
    border-radius: 4px;
  }

  i {
    width: 4px;
    height: 4px;
    background: #77b990;
    border-radius: 50%;
  }
}

.mock-answer {
  margin-left: 30px;
  padding: 12px;
  background: #fff;
  border: 1px solid #e7ebed;
  border-radius: 9px;
  box-shadow: 0 8px 18px rgb(35 50 63 / 4%);
}

.mock-line {
  width: 88%;
  height: 4px;
  margin-top: 6px;
  background: #e9edef;
  border-radius: 3px;

  &--wide {
    width: 100%;
    margin-top: 0;
  }

  &--short {
    width: 58%;
  }
}

.mock-file {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px;
  background: #f7f9f9;
  border: 1px solid #edf0f1;
  border-radius: 6px;

  div {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 2px;
  }

  strong {
    color: #48545f;
    font-size: 7px;
  }

  small {
    color: #9aa4ab;
    font-size: 6px;
  }
}

.file-icon {
  display: grid;
  width: 24px;
  height: 27px;
  color: #5a6874;
  font-size: 6px;
  font-weight: 700;
  place-items: center;
  background: #eaf0f1;
  border-radius: 3px;
}

.file-check {
  color: #6eaf86;
  font-size: 9px;
}

.mock-composer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 37px;
  margin: 0 24px 16px;
  padding: 0 11px;
  color: #a4adb4;
  font-size: 7px;
  border: 1px solid #dde3e5;
  border-radius: 7px;

  b {
    display: grid;
    width: 20px;
    height: 20px;
    color: #fff;
    font-size: 9px;
    place-items: center;
    background: $night;
    border-radius: 5px;
  }
}

.floating-card {
  position: absolute;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 176px;
  padding: 11px 13px;
  background: rgb(255 255 255 / 92%);
  border: 1px solid rgb(40 55 68 / 11%);
  border-radius: 11px;
  box-shadow: 0 18px 40px rgb(25 40 53 / 14%);
  backdrop-filter: blur(12px);
  animation: float 4s ease-in-out infinite;

  strong,
  small {
    display: block;
  }

  strong {
    font-size: 10px;
  }

  small {
    margin-top: 2px;
    color: #828d96;
    font-size: 7px;
  }

  &--tools {
    right: -12px;
    bottom: 104px;
    animation-delay: -1.2s;
  }

  &--local {
    bottom: 42px;
    left: -8px;
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-5px);
  }
}

.floating-icon {
  display: grid;
  width: 30px;
  height: 30px;
  color: $night;
  place-items: center;
  background: $lime;
  border-radius: 8px;

  svg {
    width: 15px;
  }
}

.floating-pulse {
  position: relative;
  width: 9px;
  height: 9px;
  margin: 0 10px 0 7px;
  background: #69b986;
  border-radius: 50%;

  &::after {
    position: absolute;
    inset: -5px;
    content: '';
    border: 1px solid rgb(105 185 134 / 35%);
    border-radius: 50%;
  }
}

.proof-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  width: min(calc(100% - 64px), var(--site-width));
  margin: 0 auto;
  overflow: hidden;
  background: rgb(255 255 255 / 72%);
  border: 1px solid $line;
  border-radius: 16px;
  box-shadow: 0 18px 45px rgb(31 46 58 / 4%);
  backdrop-filter: blur(10px);

  div {
    display: flex;
    flex-direction: column;
    gap: 5px;
    padding: 25px 26px;
    border-right: 1px solid $line;

    &:last-child {
      border-right: 0;
    }
  }

  strong {
    font-size: 12px;
    font-weight: 650;
  }

  span {
    color: #7d8791;
    font-size: 10px;
  }
}

.section {
  width: min(calc(100% - 64px), var(--site-width));
  margin: 0 auto;
  padding: clamp(84px, 8vw, 112px) 0;
}

.section-heading,
.download-heading,
.release-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 60px;
}

.section-heading h2,
.download-heading h2,
.release-heading h2 {
  margin-top: 18px;
  font-size: clamp(34px, 4vw, 52px);
  font-weight: 520;
  line-height: 1.16;
  letter-spacing: -0.045em;
}

.section-heading > p {
  max-width: 420px;
  padding-bottom: 6px;
  color: $muted;
  font-size: 13px;
  line-height: 1.9;
}

.feature-grid {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 18px;
  margin-top: 52px;
}

.feature-card {
  position: relative;
  min-height: 276px;
  padding: 28px;
  overflow: hidden;
  background: $white;
  border: 1px solid #e2e7e9;
  border-radius: 18px;
  box-shadow: 0 12px 32px rgb(35 49 61 / 3%);
  transition:
    transform 0.25s,
    box-shadow 0.25s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 24px 50px rgb(34 49 62 / 8%);
  }

  h3 {
    margin-top: 22px;
    font-size: 19px;
    font-weight: 600;
    letter-spacing: -0.02em;
  }

  p {
    max-width: 430px;
    margin-top: 10px;
    color: $muted;
    font-size: 13px;
    line-height: 1.75;
  }
}

.feature-card--hero {
  grid-row: span 2;
  min-height: 570px;
  color: #edf7f9;
  background:
    radial-gradient(circle at 85% 5%, rgb(130 219 231 / 20%), transparent 32%),
    $night;
  border-color: $night;

  p {
    color: #9cabb7;
  }

  .feature-index {
    color: #7f909d;
  }
}

.feature-icon {
  display: grid;
  width: 38px;
  height: 38px;
  color: $ink;
  place-items: center;
  background: $lime;
  border-radius: 10px;

  svg {
    width: 18px;
  }
}

.feature-card--hero .feature-icon {
  color: $cyan;
  background: #1c2a39;
}

.feature-index {
  position: absolute;
  top: 34px;
  right: 28px;
  color: #a8b1b7;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.branch-demo {
  position: relative;
  height: 218px;
  margin-top: 42px;

  svg {
    position: absolute;
    inset: 10px 0 auto;
    width: 100%;
    height: 100px;
    fill: none;
    stroke: #354656;
    stroke-width: 1;
  }
}

.branch-node {
  position: absolute;
  z-index: 1;
  padding: 8px 11px;
  color: #8f9eaa;
  font-size: 8px;
  background: #172331;
  border: 1px solid #2c3b4b;
  border-radius: 6px;

  &:first-child {
    top: 1px;
    left: 8px;
  }

  &:nth-child(2) {
    top: 68px;
    left: 45%;
  }

  &:nth-child(3) {
    right: 6px;
    bottom: 16px;
  }
}

.branch-node--active {
  color: $night;
  background: $cyan;
  border-color: $cyan;
}

.permission-demo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 28px;
  padding: 11px 13px;
  color: #71808b;
  font-size: 9px;
  background: #f4f7f7;
  border: 1px solid #e8edee;
  border-radius: 7px;

  b {
    padding: 5px 8px;
    color: $night;
    font-size: 8px;
    background: $lime;
    border-radius: 4px;
  }
}

.tool-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 25px;

  span {
    padding: 7px 10px;
    color: #687580;
    font-size: 8px;
    background: #f5f7f7;
    border: 1px solid #e6ebec;
    border-radius: 999px;
  }
}

.feature-card--quiet {
  background: #eef2f1;
}

.download-section {
  width: 100%;
  padding-right: max(32px, calc((100% - var(--site-width)) / 2));
  padding-left: max(32px, calc((100% - var(--site-width)) / 2));
  color: #e9f2f5;
  background:
    radial-gradient(circle at 82% 5%, rgb(130 219 231 / 9%), transparent 27%),
    $night;

  .section-kicker {
    color: #7f8e9b;
  }
}

.download-heading {
  align-items: center;
}

.channel-note {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 260px;
  padding: 15px 18px;
  background: #182534;
  border: 1px solid #293849;
  border-radius: 10px;

  strong,
  small {
    display: block;
  }

  strong {
    color: #e3edf1;
    font-size: 11px;
  }

  small {
    margin-top: 4px;
    color: #7f909e;
    font-size: 8px;
  }
}

.load-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 28px;
  padding: 12px 15px;
  color: #bac7d0;
  font-size: 11px;
  background: #182534;
  border: 1px solid #334554;
  border-radius: 8px;

  svg {
    width: 15px;
  }

  button {
    margin-left: auto;
    color: $cyan;
    font: inherit;
    border: 0;
    background: transparent;
    cursor: pointer;
  }
}

.download-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 42px;
}

.download-card {
  position: relative;
  display: flex;
  min-height: 258px;
  flex-direction: column;
  padding: 24px;
  overflow: hidden;
  background:
    radial-gradient(circle at 100% 0%, rgb(130 219 231 / 7%), transparent 42%),
    #172331;
  border: 1px solid #2a3949;
  border-radius: 17px;
  box-shadow: 0 18px 40px rgb(3 10 18 / 15%);
  transition:
    border-color 0.2s,
    transform 0.2s,
    box-shadow 0.2s;

  &::after {
    position: absolute;
    top: 0;
    right: 24px;
    left: 24px;
    height: 1px;
    content: '';
    background: linear-gradient(90deg, transparent, rgb(130 219 231 / 34%), transparent);
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover {
    border-color: #405363;
    transform: translateY(-3px);
    box-shadow: 0 24px 48px rgb(3 10 18 / 22%);

    &::after {
      opacity: 1;
    }
  }

  &.is-recommended {
    background:
      linear-gradient(150deg, rgb(130 219 231 / 8%), transparent 50%),
      #182635;
    border-color: #466575;

    &::after {
      opacity: 1;
    }
  }

  &.is-unavailable {
    min-height: 225px;
  }

  h3 {
    margin-top: 23px;
    font-size: 22px;
    font-weight: 560;
  }

  > p {
    margin-top: 6px;
    color: #8292a0;
    font-size: 10px;
  }
}

.download-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.platform-mark {
  display: grid;
  width: 36px;
  height: 36px;
  color: #cae6eb;
  font-size: 16px;
  font-weight: 650;
  place-items: center;
  background: #223141;
  border-radius: 8px;

  &--win {
    grid-template-columns: repeat(2, 7px);
    grid-template-rows: repeat(2, 7px);
    gap: 2px;
    place-content: center;

    i {
      width: 7px;
      height: 7px;
      background: #8fdbe5;
    }
  }

  &--mac {
    font-size: 20px;
  }

  &--linux {
    font-family: Consolas, monospace;
    font-size: 10px;
    letter-spacing: -0.12em;
  }
}

.recommended-tag {
  padding: 4px 7px;
  color: $cyan;
  font-size: 7px;
  font-weight: 700;
  letter-spacing: 0.06em;
  border: 1px solid #36505f;
  border-radius: 999px;
}

.download-version {
  display: flex;
  align-items: baseline;
  gap: 9px;
  margin-top: auto;
  padding-top: 24px;

  strong {
    color: #dce9ed;
    font-size: 13px;
    font-weight: 600;
  }

  span {
    color: #778896;
    font-size: 8px;
  }
}

.download-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  min-height: 38px;
  margin-top: 14px;
}

.platform-download {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 14px;
  color: $night;
  font: inherit;
  font-size: 10px;
  font-weight: 650;
  background: $cyan;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  transition:
    background 0.2s,
    transform 0.2s;

  &:hover {
    background: #a7e8f0;
    transform: translateY(-1px);
  }
}

.download-empty {
  display: flex;
  align-items: center;
  height: 38px;
  color: #758694;
  font-size: 9px;
}

.download-footnote {
  margin-top: 22px;
  color: #70818e;
  font-size: 9px;
  line-height: 1.7;
}

.release-heading {
  align-items: flex-end;
}

.latest-pill {
  margin-bottom: 8px;
  padding: 7px 11px;
  color: #4f5e69;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.1em;
  border: 1px solid #cfd7da;
  border-radius: 999px;
}

.history-note {
  margin-top: 26px;
  padding: 13px 16px;
  color: #7a683c;
  font-size: 10px;
  background: #faf6e9;
  border: 1px solid #eee4c8;
  border-radius: 8px;
}

.release-list {
  margin-top: 44px;
  overflow: hidden;
  background: rgb(255 255 255 / 72%);
  border: 1px solid $line;
  border-radius: 18px;
  box-shadow: 0 16px 40px rgb(31 46 58 / 4%);
}

.release-item {
  & + .release-item {
    border-top: 1px solid $line;
  }

  summary {
    display: grid;
    grid-template-columns: 110px 110px 48px 1fr 32px;
    align-items: center;
    gap: 16px;
    min-height: 80px;
    padding: 0 22px;
    cursor: pointer;
    list-style: none;
    transition: background-color 0.2s;

    &:hover {
      background: #fafcfb;
    }

    &::-webkit-details-marker {
      display: none;
    }
  }

  &[open] summary {
    background: #fafcfb;
  }

  &[open] .release-toggle {
    color: $ink;
    background: #edf2f1;
    transform: rotate(180deg);
  }
}

.release-date {
  grid-column: 1;
  color: #7d8791;
  font-size: 10px;
}

.release-version {
  grid-column: 2;
  color: $ink;
  font-size: 19px;
  font-weight: 620;
  letter-spacing: -0.02em;
}

.release-latest {
  grid-column: 3;
  width: max-content;
  padding: 3px 6px;
  color: #48635c;
  font-size: 7px;
  font-weight: 700;
  background: #dff0e5;
  border-radius: 3px;
}

.release-platforms {
  display: flex;
  grid-column: 4;
  flex-wrap: wrap;
  gap: 5px;

  i {
    padding: 4px 7px;
    color: #6f7c86;
    font-size: 8px;
    font-style: normal;
    background: #edf1f1;
    border-radius: 4px;
  }
}

.release-toggle {
  display: grid;
  width: 28px;
  height: 28px;
  grid-column: 5;
  justify-self: end;
  color: #7f8992;
  place-items: center;
  background: #f2f5f4;
  border: 1px solid #e3e8e9;
  border-radius: 50%;
  transition:
    color 0.2s,
    background-color 0.2s,
    transform 0.25s;

  svg {
    width: 13px;
    height: 13px;
    fill: none;
    stroke: currentColor;
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke-width: 1.6;
  }
}

.release-content {
  padding: 0 48px 30px 238px;
  background: #fafcfb;

  p {
    max-width: 760px;
    color: #61707c;
    font-size: 12px;
    line-height: 1.9;
    white-space: pre-wrap;
  }
}

.release-skeletons {
  padding: 22px;

  span {
    display: block;
    width: 100%;
    height: 46px;
    margin-top: 8px;
    background: linear-gradient(90deg, #edf0f0 25%, #f7f8f8 50%, #edf0f0 75%);
    background-size: 200% 100%;
    border-radius: 7px;
    animation: skeleton 1.4s infinite linear;
  }
}

@keyframes skeleton {
  to {
    background-position: -200% 0;
  }
}

.release-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  flex-direction: column;
  gap: 7px;
  margin-top: 35px;
  color: #66737e;
  border: 1px dashed #cfd7da;
  border-radius: 12px;

  span {
    font-size: 13px;
  }

  small {
    color: #919ba2;
    font-size: 10px;
  }
}

.release-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 28px;
}

.release-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 15px;
  color: #586570;
  font: inherit;
  font-size: 11px;
  background: rgb(255 255 255 / 62%);
  border: 1px solid #ced7da;
  border-radius: 999px;
  cursor: pointer;
  transition:
    color 0.2s,
    background-color 0.2s,
    border-color 0.2s;

  &:hover {
    color: $ink;
    background: $white;
    border-color: #aeb9be;
  }

  &:disabled {
    cursor: wait;
    opacity: 0.65;
  }

  .el-icon {
    transform: rotate(90deg);
    transition: transform 0.2s;

    &.is-up {
      transform: rotate(-90deg);
    }
  }
}

.release-action--primary {
  color: #edf8fa;
  background: $night;
  border-color: $night;

  &:hover {
    color: #fff;
    background: #1b2a39;
    border-color: #1b2a39;
  }
}

.release-range {
  width: 100%;
  color: #929ba2;
  font-size: 9px;
  text-align: center;
}

.release-load-error {
  margin-top: 14px;
  color: #9a6c39;
  font-size: 10px;
  text-align: center;

  button {
    margin-left: 7px;
    padding: 0;
    color: #7b5b32;
    font: inherit;
    font-weight: 600;
    border: 0;
    background: transparent;
    cursor: pointer;
  }
}

.closing {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 440px;
  padding: 64px 32px;
  overflow: hidden;
  flex-direction: column;
  justify-content: center;
  color: #eef7f9;
  text-align: center;
  background:
    radial-gradient(circle at 50% 0%, rgb(130 219 231 / 17%), transparent 35%),
    $night;

  .section-kicker {
    position: relative;
    z-index: 1;
    color: #8595a2;
  }

  h2 {
    position: relative;
    z-index: 1;
    margin-top: 20px;
    font-size: clamp(38px, 5vw, 64px);
    font-weight: 500;
    line-height: 1.12;
    letter-spacing: -0.05em;
  }

  p {
    position: relative;
    z-index: 1;
    margin-top: 17px;
    color: #90a0ad;
    font-size: 12px;
  }
}

.closing-orbit {
  position: absolute;
  top: -20px;
  left: 50%;
  width: 630px;
  height: 630px;
  border: 1px solid rgb(130 219 231 / 12%);
  border-radius: 50%;
  transform: translateX(-50%);

  &::before,
  &::after {
    position: absolute;
    content: '';
    border: 1px solid rgb(130 219 231 / 8%);
    border-radius: 50%;
  }

  &::before {
    inset: 72px;
  }

  &::after {
    inset: 145px;
  }
}

.primary-action--light {
  position: relative;
  z-index: 1;
  min-height: 46px;
  margin-top: 28px;
  color: $night;
  background: $cyan;
  box-shadow: 0 12px 32px rgb(130 219 231 / 14%);

  &:hover:not(:disabled) {
    background: #a7e8f0;
  }
}

.site-footer {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  width: min(calc(100% - 64px), var(--site-width));
  min-height: 112px;
  margin: 0 auto;
  color: #78838d;
  font-size: 10px;

  > p {
    letter-spacing: 0.08em;
  }
}

.site-brand--footer {
  .brand-mark {
    width: 32px;
    height: 32px;
    border-radius: 9px;

    svg {
      width: 26px;
    }
  }

  .brand-copy strong {
    font-size: 14px;
  }

  .brand-copy small {
    font-size: 9px;
  }
}

.footer-links {
  display: flex;
  justify-self: end;
  gap: 20px;

  a {
    color: #6d7882;

    &:hover {
      color: $ink;
    }
  }
}

@media (max-width: 1080px) {
  .hero {
    grid-template-columns: 1fr;
    min-height: auto;
    padding-top: 54px;
    padding-bottom: 70px;
  }

  .hero-copy {
    max-width: 760px;
  }

  .hero-visual {
    width: min(100%, 760px);
    min-height: 510px;
    margin: 0 auto;
  }

  .proof-strip {
    grid-template-columns: repeat(2, 1fr);

    div:nth-child(2) {
      border-right: 0;
    }

    div:nth-child(-n + 2) {
      border-bottom: 1px solid $line;
    }
  }
}

@media (max-width: 820px) {
  .hero,
  .proof-strip,
  .section,
  .site-footer {
    width: min(calc(100% - 40px), var(--site-width));
  }

  .site-nav {
    grid-template-columns: auto 1fr auto;
    height: 68px;
    padding: 0 20px;
  }

  .nav-links {
    justify-self: center;
    gap: 16px;

    a {
      font-size: 11px;
    }
  }

  .hero {
    padding-top: 38px;
    padding-bottom: 58px;
  }

  .hero h1 {
    font-size: clamp(42px, 8vw, 56px);
  }

  .hero-visual {
    min-height: 430px;
  }

  .product-window {
    width: 600px;
    transform: translateX(-50%) scale(0.84) rotate(-1deg);
    transform-origin: top center;
  }

  .floating-card--tools {
    right: 0;
    bottom: 35px;
  }

  .floating-card--local {
    bottom: 6px;
  }

  .orbit--outer {
    width: 470px;
    height: 470px;
  }

  .orbit--inner {
    width: 350px;
    height: 350px;
  }

  .section {
    padding: 80px 0;
  }

  .section-heading,
  .download-heading,
  .release-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 24px;
  }

  .section-heading > p {
    max-width: 620px;
  }

  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feature-card--hero {
    min-height: 460px;
    grid-row: auto;
    grid-column: 1 / -1;
  }

  .download-section {
    width: 100%;
    padding-right: 20px;
    padding-left: 20px;
  }

  .download-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
  }

  .download-card {
    min-height: 250px;
    padding: 20px;
  }

  .download-card.is-unavailable {
    min-height: 210px;
  }

  .download-card h3 {
    font-size: 19px;
  }

  .download-card > p {
    min-height: 30px;
  }

  .release-item summary {
    grid-template-columns: 86px 1fr 28px;
  }

  .release-date {
    grid-column: 1;
    grid-row: 1;
  }

  .release-version {
    grid-column: 2;
    grid-row: 1;
  }

  .release-platforms {
    grid-column: 2;
    grid-row: 2;
    padding-bottom: 14px;
  }

  .release-latest {
    display: none;
  }

  .release-toggle {
    grid-column: 3;
    grid-row: 1 / span 2;
  }

  .release-content {
    padding: 0 28px 26px 102px;
  }

  .site-footer {
    grid-template-columns: 1fr auto;
    gap: 20px;
    padding: 26px 0;

    > p {
      display: none;
    }
  }
}

@media (max-width: 680px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }

  .feature-card--hero {
    min-height: 440px;
    grid-column: auto;
  }

  .download-grid {
    grid-template-columns: 1fr;
  }

  .download-card {
    min-height: 225px;
  }

  .download-card.is-unavailable {
    min-height: 185px;
  }

  .download-card > p {
    min-height: 0;
  }
}

@media (max-width: 560px) {
  .hero,
  .proof-strip,
  .section,
  .site-footer {
    width: calc(100% - 32px);
  }

  .brand-copy small {
    display: none;
  }

  .nav-console {
    font-size: 11px;
  }

  .site-nav {
    padding: 0 16px;
  }

  .nav-links {
    gap: 10px;
  }

  .hero {
    min-height: 0;
    padding-bottom: 56px;
  }

  .hero h1 {
    font-size: 34px;
  }

  .hero-lead {
    font-size: 13px;
  }

  .hero-actions {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }

  .primary-action,
  .secondary-action {
    width: 100%;
  }

  .hero-meta {
    align-items: flex-start;
    flex-wrap: wrap;
    line-height: 1.7;
  }

  .hero-visual {
    min-height: 360px;
    margin-top: 0;
  }

  .product-window {
    top: 24px;
    transform: translateX(-50%) scale(0.62) rotate(-1deg);
  }

  .floating-card {
    min-width: 160px;

    &--tools {
      right: -2px;
      bottom: 18px;
    }

    &--local {
      bottom: -10px;
      left: 0;
    }
  }

  .proof-strip {
    div {
      padding: 18px 14px;
    }
  }

  .section-heading h2,
  .download-heading h2,
  .release-heading h2 {
    font-size: 34px;
  }

  .feature-card {
    padding: 24px;
  }

  .feature-card--hero {
    min-height: 420px;
  }

  .branch-demo {
    height: 165px;
    margin-top: 26px;
  }

  .feature-index {
    top: 30px;
    right: 24px;
  }

  .channel-note {
    width: 100%;
  }

  .latest-pill {
    display: none;
  }

  .release-item summary {
    grid-template-columns: 74px 1fr 28px;
    gap: 8px;
  }

  .release-version {
    font-size: 17px;
  }

  .release-content {
    padding-left: 82px;
  }

  .release-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .release-action {
    width: 100%;
  }

  .closing {
    min-height: 420px;
  }

  .closing h2 {
    font-size: 38px;
  }

  .site-footer {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .footer-links {
    justify-self: center;
  }
}

@media (max-width: 400px) {
  .site-nav {
    grid-template-columns: 1fr auto;
  }

  .nav-links {
    display: none;
  }

  .hero h1 {
    font-size: 29px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .product-window,
  .floating-card,
  .release-skeletons span {
    animation: none;
  }

  .feature-card,
  .download-card {
    transition: none;
  }
}
</style>
