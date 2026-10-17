<script lang="ts">
/** 版本清单里的一行：任何「带版本号的对象」都能喂给选择器（分享的项目版本、技能版本…） */
export interface VersionOption {
  id: number
  /** 版本号：标签与清单里显示的就是它 */
  name: string
  description?: string
  /** 1 启用；其它值在清单里标「已停用」，不传就不判断 */
  status?: number
  createdTime?: number
}

/** 取一页版本的入参：检索框与「加载更多」都按这个形状向数据源要数据 */
export interface VersionQuery {
  page: number
  pageSize: number
  /** 检索词，空串表示不筛选 */
  name: string
}

export interface VersionPage {
  rows: VersionOption[]
  total: number
}
</script>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import LayoutIcon from '@renderer/components/Layout/LayoutIcon.vue'
import { formatDate } from '@renderer/utils/TimeUtil'
import { errorMessage } from '@renderer/utils/ErrorUtil'

/**
 * 版本选择器（公共组件）：卡片右上角的一枚版本标签，点开是可检索的版本清单。
 *
 * 「版本从哪来」交给 loader：分享卡片按页问服务端要（检索也在服务端做），技能卡片一次取回某个技能的
 * 全部版本（检索在 loader 里本地过滤）——组件只管交互与样式，不关心版本属于谁。
 * 选中哪一版由调用方决定用途：分享卡片据它导出 / 创建本地项目，技能卡片据它下载安装。
 * 清单在面板里限高滚动（下拉菜单本来就是这个交互，不占卡片高度、也不会从页面抢走滚轮）。
 */

const props = withDefaults(
  defineProps<{
    /** 取一页版本：展开面板、输入检索词、点「加载更多」都会走它 */
    loader: (query: VersionQuery) => Promise<VersionPage>
    /** 当前版本：标签上显示它的版本号，清单里给它打 ✓ */
    current?: VersionOption | null
    /** 已经落地的版本号（如已安装的技能版本）：清单里额外标一枚「已安装」 */
    applied?: string
    /** 没有可切换的版本时置灰（技能只有一版、项目没有版本） */
    disabled?: boolean
    /** 面板一次列几个 */
    pageSize?: number
    /** 没有当前版本时标签上的占位文案 */
    placeholder?: string
    /** 检索框占位文案 */
    searchPlaceholder?: string
    /** 清单为空时的文案 */
    emptyText?: string
  }>(),
  {
    current: null,
    applied: '',
    disabled: false,
    pageSize: 10,
    placeholder: '无版本',
    searchPlaceholder: '按版本号检索',
    emptyText: '暂无版本记录'
  }
)

const emit = defineEmits<{
  select: [VersionOption]
}>()

const keyword = ref('')
const rows = ref<VersionOption[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')
let timer: ReturnType<typeof setTimeout> | undefined

const remaining = computed(() => Math.max(0, total.value - rows.value.length))

/** 当前版本是否已停用：停用的版本标签退回灰底，标题里也点一句 */
const stopped = computed(() => undefined !== props.current?.status && props.current?.status !== 1)

async function load(reset: boolean): Promise<void> {
  const next = reset ? 1 : page.value + 1
  if (reset) loading.value = true
  else loadingMore.value = true
  error.value = ''
  try {
    const result = await props.loader({
      page: next,
      pageSize: props.pageSize,
      name: keyword.value.trim()
    })
    rows.value = reset ? result.rows : [...rows.value, ...result.rows]
    total.value = result.total
    page.value = next
  } catch (cause) {
    // 数据源可能已经弹过提示；面板里再保留一句失败原因，收起再打开也能看见
    error.value = errorMessage(cause, '版本记录加载失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

/** 每次展开都按当前检索词重新取第一页：收起期间新发布的版本也能看到 */
function onVisible(visible: boolean): void {
  if (visible) void load(true)
}

/** 检索框：输入停顿一下再查，不要每敲一个字就发一次请求 */
watch(keyword, () => {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => void load(true), 250)
})

/** 离开页面时把待触发的检索收掉，别让定时器打在已经卸载的组件上 */
onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})

/** 标签上的悬停说明：当前版本的说明与发布日期 */
const tagTitle = computed(() => {
  const version = props.current
  if (!version) return props.disabled ? '暂无版本可切换' : '点击查看版本记录'
  const date = formatDate(version.createdTime ?? 0)
  const prefix = stopped.value ? '已停用 · ' : ''
  return `${prefix}${version.description || '未填写版本说明'}${date ? `（${date}）` : ''}`
})
</script>

<template>
  <el-dropdown
    trigger="click"
    :disabled="disabled"
    :hide-on-click="true"
    @visible-change="onVisible"
    @command="(version: VersionOption) => emit('select', version)"
  >
    <el-tag
      class="version-tag"
      :class="{ 'is-static': disabled }"
      size="small"
      :type="current && !stopped ? 'success' : 'info'"
      :effect="current && !stopped ? 'light' : 'plain'"
      :title="tagTitle"
    >
      <span class="version-text">{{ current ? `v${current.name}` : placeholder }}</span>
      <span v-if="!disabled" class="version-caret">▾</span>
    </el-tag>
    <template #dropdown>
      <div class="version-panel">
        <div class="panel-search">
          <el-input v-model="keyword" size="small" :placeholder="searchPlaceholder" clearable>
            <template #prefix>
              <LayoutIcon name="plus.search" class="search-icon" />
            </template>
          </el-input>
        </div>
        <div v-if="error" class="panel-error">
          <span>{{ error }}</span>
          <el-button text type="primary" size="small" @click="load(true)">重试</el-button>
        </div>
        <el-scrollbar max-height="260px">
          <div v-if="loading" class="panel-empty">正在读取版本记录…</div>
          <template v-else-if="rows.length">
            <el-dropdown-menu class="version-menu">
              <el-dropdown-item v-for="version in rows" :key="version.id" :command="version">
                <span class="version-item" :class="{ 'is-current': current?.id === version.id }">
                  <span class="version-item-head">
                    <span class="version-check">{{ current?.id === version.id ? '✓' : '' }}</span>
                    <!-- 版本号可能很长：截断后靠 title 看全 -->
                    <span class="version-item-name" :title="`v${version.name}`">
                      v{{ version.name }}
                    </span>
                    <el-tag
                      v-if="undefined !== version.status && version.status !== 1"
                      size="small"
                      type="info"
                    >
                      已停用
                    </el-tag>
                    <el-tag
                      v-if="applied && applied === version.name"
                      size="small"
                      type="success"
                      effect="plain"
                    >
                      已安装
                    </el-tag>
                    <span
                      class="version-item-date"
                      :title="'发布时间：' + formatDate(version.createdTime ?? 0)"
                    >
                      {{ formatDate(version.createdTime ?? 0) }}
                    </span>
                  </span>
                  <!-- 版本说明只显示一行，悬停看全文（title 挂在这一行上） -->
                  <span
                    v-if="version.description"
                    class="version-item-desc"
                    :title="version.description"
                  >
                    {{ version.description }}
                  </span>
                </span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
          <div v-else-if="!error" class="panel-empty">
            {{ keyword.trim() ? '没有匹配的版本' : emptyText }}
          </div>
        </el-scrollbar>
        <div v-if="rows.length" class="panel-footer">
          <span class="panel-count">{{
            keyword.trim() ? `匹配 ${total} 个版本` : `共 ${total} 个版本`
          }}</span>
          <el-button
            v-if="remaining > 0"
            text
            type="primary"
            size="small"
            :loading="loadingMore"
            @click="load(false)"
          >
            加载更多（还有 {{ remaining }} 个）
          </el-button>
        </div>
      </div>
    </template>
  </el-dropdown>
</template>

<style lang="scss" scoped>
/* 版本标签：一枚带下拉小箭头的标签，悬停高亮，没有可切换的版本时置灰 */
.version-tag {
  flex: none;
  cursor: pointer;
  transition:
    color 0.2s,
    border-color 0.2s,
    background-color 0.2s;

  .version-text {
    font-variant-numeric: tabular-nums;
  }

  .version-caret {
    margin-left: 3px;
    font-size: 10px;
    line-height: 1;
  }

  &:not(.is-static):hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary);
  }

  &.is-static {
    cursor: default;
  }
}

.version-panel {
  width: 320px;
}

.panel-search {
  padding: 8px 10px 6px;
}

.panel-error {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 12px 8px;
  color: var(--el-color-warning);
  font-size: 12px;
  line-height: 1.45;

  span {
    flex: 1;
  }
}

.search-icon {
  font-size: 13px;
  color: var(--ev-c-text-3);
}

.panel-empty {
  padding: 10px 12px 16px;
  font-size: 12px;
  color: var(--ev-c-text-3);
}

.version-menu {
  padding: 0 4px;
}

/* 行：版本号 + 标记 + 日期一行，说明第二行；当前版本整行高亮 */
.version-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  max-width: 268px;
  padding: 2px 0;
}

.version-item-head {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.version-check {
  width: 12px;
  color: var(--el-color-success);
}

.version-item-name {
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--ev-c-text-1);
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-item-date {
  margin-left: auto;
  font-size: 12px;
  color: var(--ev-c-text-3);
  font-variant-numeric: tabular-nums;
}

.version-item-desc {
  padding-left: 18px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--ev-c-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-item.is-current {
  .version-item-name {
    color: var(--el-color-success);
  }
}

.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.panel-count {
  font-size: 12px;
  color: var(--ev-c-text-3);
}
</style>
