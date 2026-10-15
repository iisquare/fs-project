<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useProjectStore } from '@renderer/stores/project'
import { useShareStore } from '@renderer/stores/share'
import { configOf, filenameOf, projectFile, uniqueName } from '@renderer/utils/ProjectUtil'
import { formatDate } from '@renderer/utils/TimeUtil'
import VersionSelect from '@renderer/components/Version/VersionSelect.vue'
import type {
  VersionOption,
  VersionPage,
  VersionQuery
} from '@renderer/components/Version/VersionSelect.vue'
import type { ShareProject } from '@renderer/stores/share'

/**
 * 分享项目的列表：项目管理页的「我的分享」与「浏览分享」两个页签共用这一份。
 *
 * - 我的分享：自己发布过的项目，可以继续追加版本（交给页面打开分享表单）、导出任一版本的配置；
 * - 浏览分享：其他用户分享的项目（服务端只给已启用且授权命中的），可以导出配置或创建成本地项目。
 *
 * 版本号收在卡片右上角的下拉里（公共组件 `VersionSelect`，与技能卡片同一套交互，可检索）：
 * 选中哪一版，卡片上的「导出配置 / 创建本地项目」就作用于哪一版，默认是最新版本。
 * 列表本身按页取（每页 30），取不完时给「加载更多」。
 */

const props = defineProps<{ mode: 'mine' | 'browse' }>()

const emit = defineEmits<{
  /** 在我的分享里点「分享新版本」：由页面打开分享表单（带上这个分享项目作为目标） */
  newVersion: [ShareProject]
  /** 从别人的项目创建了本地项目：由页面切回「我的项目」页签 */
  created: []
}>()

const projectStore = useProjectStore()
const shareStore = useShareStore()

const mine = computed(() => 'mine' === props.mode)
const rows = computed(() => (mine.value ? shareStore.mine : shareStore.browse))
const loading = computed(() => (mine.value ? shareStore.loadingMine : shareStore.loadingBrowse))
/** 还没取到的项目数：总数在服务端，列表只铺了当前已取回的这些 */
const remaining = computed(() =>
  Math.max(0, (mine.value ? shareStore.mineTotal : shareStore.browseTotal) - rows.value.length)
)

const keyword = ref('')
/** 各分享项目当前选中的版本：没选过就按最新版本（列表行给的 latestVersion） */
const picked = ref<Record<number, VersionOption | null>>({})
const loadingMore = ref(false)
const exporting = ref(0)
const creating = ref(0)

/** 卡片要用的「项目 + 当前版本」：模板里按它渲染，省得每处都算一遍选中版本 */
const cards = computed(() =>
  rows.value.map((item) => ({
    item,
    version: (picked.value[item.id] ?? item.latestVersion) as VersionOption | null,
    loader: versionLoader(item)
  }))
)

/** 版本清单的数据源：分页与「按版本号检索」都在服务端做（版本攒得再多也搜得动） */
function versionLoader(project: ShareProject): (query: VersionQuery) => Promise<VersionPage> {
  return async (query) => {
    const page = await shareStore.fetchVersions(project.id, {
      page: query.page,
      pageSize: query.pageSize,
      name: query.name
    })
    return { rows: page.rows, total: page.total }
  }
}

onMounted(() => {
  void refresh()
})

async function refresh(): Promise<void> {
  if (mine.value) await shareStore.fetchMine(keyword.value.trim())
  else await shareStore.fetchBrowse(keyword.value.trim())
}

/** 续取下一页项目：可见范围与总数都在服务端，页数按已取到的条数推 */
async function loadMore(): Promise<void> {
  loadingMore.value = true
  try {
    if (mine.value) await shareStore.loadMoreMine(keyword.value.trim())
    else await shareStore.loadMoreBrowse(keyword.value.trim())
  } finally {
    loadingMore.value = false
  }
}

/** 从右上角的下拉里挑了一版：卡片上的动作随后就作用于它 */
function selectVersion(project: ShareProject, version: VersionOption): void {
  picked.value = { ...picked.value, [project.id]: version }
}

/** 导出某个版本的项目配置：与服务端上存的那份一致，带来源信息，可直接用本客户端导入 */
async function exportVersion(project: ShareProject, version: VersionOption | null): Promise<void> {
  if (!version) return
  exporting.value = version.id
  try {
    const detail = await shareStore.fetchVersion(version.id)
    if (!detail) return
    const file = projectFile(configOf(detail.version?.content ?? {}), {
      projectId: project.id,
      projectName: project.name,
      versionId: version.id,
      versionName: version.name,
      description: version.description ?? ''
    })
    const saved = await window.ipc.project.exportFile(
      filenameOf(project.name, version.name),
      JSON.stringify(file, null, 2)
    )
    if (saved.path) ElMessage.success(`已导出到 ${saved.path}`)
  } catch (error) {
    ElMessage.error(`导出失败：${error instanceof Error ? error.message : String(error)}`)
  } finally {
    exporting.value = 0
  }
}

/** 把别人分享的某个版本创建成本地项目：重名时自动加「（导入）」后缀 */
async function createLocal(project: ShareProject, version: VersionOption | null): Promise<void> {
  if (!version) return
  try {
    await ElMessageBox.confirm(
      `把「${project.name}」的 v${version.name} 创建成本地项目？模型、工具、环境变量与工作区目录会一并带过来。`,
      '创建本地项目',
      { type: 'info' }
    )
  } catch {
    return
  }
  creating.value = version.id
  try {
    const detail = await shareStore.fetchVersion(version.id)
    if (!detail) return
    const config = configOf(detail.version?.content ?? {})
    config.name = uniqueName(
      config.name || project.name,
      projectStore.projects.map((item) => item.name)
    )
    await projectStore.create(config)
    ElMessage.success(`已创建本地项目「${config.name}」`)
    // 切回「我的项目」页签看刚建的项目（页面自己处理）
    emit('created')
  } catch (error) {
    ElMessage.error(`创建失败：${error instanceof Error ? error.message : String(error)}`)
  } finally {
    creating.value = 0
  }
}
</script>

<template>
  <div class="share-list">
    <div class="share-search">
      <el-input
        v-model="keyword"
        class="search-input"
        placeholder="按项目名称搜索"
        clearable
        @keyup.enter="refresh"
      />
      <el-button :loading="loading" @click="refresh">搜索</el-button>
      <span class="search-count"
        >共 {{ mine ? shareStore.mineTotal : shareStore.browseTotal }} 个</span
      >
    </div>
    <div v-loading="loading">
      <el-empty
        v-if="cards.length === 0 && mine"
        description="还没有分享过项目，点右上角「分享项目」把本地项目发布上去"
      />
      <el-empty v-else-if="cards.length === 0" description="暂无其他用户分享的项目" />
      <div v-else class="share-grid">
        <el-card v-for="card in cards" :key="card.item.id" shadow="hover" class="share-card">
          <div class="card-top">
            <el-avatar :size="40" :src="card.item.icon">{{ card.item.name.charAt(0) }}</el-avatar>
            <div class="card-title">
              <span class="name" :title="card.item.name">{{ card.item.name }}</span>
              <div class="tag-row">
                <template v-if="mine">
                  <el-tag v-if="card.item.status !== 1" size="small" type="info">已停用</el-tag>
                  <el-tag v-else size="small" type="success">已发布</el-tag>
                </template>
                <el-tag v-else-if="card.item.mine" size="small" type="warning">我分享的</el-tag>
                <span v-if="!mine" class="author">
                  来自 {{ card.item.createdUserInfo?.name || '其他用户' }}
                </span>
              </div>
            </div>
            <!-- 版本号：右上角的下拉，点开可检索与切换；选中的那一版就是下面动作的对象 -->
            <VersionSelect
              :loader="card.loader"
              :current="card.version"
              :disabled="card.item.versionCount < 1"
              @select="selectVersion(card.item, $event)"
            />
          </div>
          <!-- 描述超过两行会截断：悬停看全文 -->
          <p class="card-desc" :title="card.item.description || ''">
            {{ card.item.description || '暂无描述' }}
          </p>
          <!-- 版本信息与按钮同一行：左边是总数与当前版本日期，右边是操作 -->
          <div class="card-actions">
            <div class="card-meta">
              <span>版本 {{ card.item.versionCount }}</span>
              <span v-if="card.version">{{ formatDate(card.version.createdTime) }}</span>
            </div>
            <el-button
              text
              type="primary"
              :disabled="!card.version"
              :loading="card.version !== null && exporting === card.version.id"
              :title="card.version ? `导出 v${card.version.name} 的项目配置` : '暂无版本可导出'"
              @click="exportVersion(card.item, card.version)"
            >
              导出配置
            </el-button>
            <el-button
              v-if="!mine"
              text
              type="primary"
              :disabled="!card.version"
              :loading="card.version !== null && creating === card.version.id"
              :title="card.version ? `把 v${card.version.name} 创建成本地项目` : '暂无版本可用'"
              @click="createLocal(card.item, card.version)"
            >
              创建本地项目
            </el-button>
            <el-button v-if="mine" text type="primary" @click="emit('newVersion', card.item)">
              分享新版本
            </el-button>
          </div>
        </el-card>
      </div>
      <!-- 项目比一页多时给「加载更多」：总数来自服务端，取完即收起 -->
      <div v-if="cards.length > 0 && remaining > 0" class="share-more">
        <el-button :loading="loadingMore" @click="loadMore">
          加载更多（还有 {{ remaining }} 个）
        </el-button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.share-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;

  .search-input {
    width: 240px;
  }

  .search-count {
    margin-left: auto;
    font-size: 12px;
    color: var(--ev-c-text-3);
  }
}

.share-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.share-more {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.share-card {
  display: flex;
  flex-direction: column;
  transition: border-color 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
  }

  :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 16px;
  }

  /* 顶部对齐：版本号贴在右上角，与项目名同一行 */
  .card-top {
    display: flex;
    align-items: flex-start;
    gap: 12px;
  }

  .card-title {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    min-width: 0;

    .name {
      font-size: 15px;
      font-weight: 500;
      color: var(--ev-c-text-1);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .tag-row {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
    }

    .author {
      font-size: 12px;
      color: var(--ev-c-text-3);
    }
  }

  .card-desc {
    margin: 12px 0;
    font-size: 13px;
    color: var(--ev-c-text-3);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 36px;
  }

  /* 版本信息与操作同一行：信息占左边（margin-right 把按钮推到右侧），按钮留在右边 */
  .card-actions {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    margin-top: auto;
    padding-top: 12px;
  }

  .card-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    min-width: 0;
    margin-right: auto;
    font-size: 12px;
    color: var(--ev-c-text-3);
  }
}
</style>
