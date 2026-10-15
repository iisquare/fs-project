<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCatalogStore } from '@renderer/stores/catalog'
import { useSkillStore } from '@renderer/stores/skill'
import VersionSelect from '@renderer/components/Version/VersionSelect.vue'
import type {
  VersionOption,
  VersionPage,
  VersionQuery
} from '@renderer/components/Version/VersionSelect.vue'

/**
 * 技能管理：技能与版本由服务端维护，本地只做「下载指定版本 → 解压到 skills 目录」。
 * 解压后的技能包放在本地 skills 目录供本机使用，不由项目引用（项目里的能力清单见「工具」）。
 *
 * 版本号收在卡片右上角的版本下拉里（公共组件 VersionSelect，与项目分享卡片同一套交互）：
 * 选中哪一版，「下载并安装」就装哪一版，默认是最新版本；清单里给已安装的那一版标出来。
 */

const activeTab = ref('marketplace')
const catalogStore = useCatalogStore()
const skillStore = useSkillStore()

const keyword = ref('')
/** 各技能当前选中的版本：没选过就按服务端给的最近启用版本（/skill/available 的 latestVersion） */
const picked = ref<Record<number, VersionOption | null>>({})
/** 技能版本清单的缓存：面板反复开合用同一份（技能版本没有检索参数，没必要每次展开都请求一遍） */
const versionCache = ref<Record<number, VersionOption[]>>({})

const filtered = computed(() => {
  const text = keyword.value.trim()
  if (!text) return catalogStore.skills
  return catalogStore.skills.filter(
    (item) => item.name.includes(text) || String(item.description ?? '').includes(text)
  )
})

/** 卡片要用的「技能 + 当前版本 + 版本数据源」：模板里按它渲染，省得每处都算一遍 */
const cards = computed(() =>
  filtered.value.map((skill: any) => {
    const version = (picked.value[skill.id] ?? skill.latestVersion ?? null) as VersionOption | null
    return {
      skill,
      version,
      loader: skillVersionLoader(skill.id),
      installed: installedHint(skill, version)
    }
  })
)

onMounted(async () => {
  await Promise.all([catalogStore.fetchSkills(), skillStore.fetchInstalled()])
})

/**
 * 技能版本清单：一次取回该技能的全部启用版本，检索词在本地过滤
 * （技能版本清单接口没有检索参数，版本量级也不大；版本面板展开时才取）。
 */
function skillVersionLoader(skillId: number) {
  return async (query: VersionQuery): Promise<VersionPage> => {
    let rows = versionCache.value[skillId]
    if (!rows) {
      rows = ((await catalogStore.fetchVersions(skillId)) ?? []) as VersionOption[]
      versionCache.value = { ...versionCache.value, [skillId]: rows }
    }
    const name = query.name.trim()
    const matched = name ? rows.filter((item) => String(item.name).includes(name)) : rows
    return { rows: matched, total: matched.length }
  }
}

/** 刷新技能清单：连同版本缓存一起清掉，新上传的版本刷一次就能看到 */
async function refreshSkills(): Promise<void> {
  versionCache.value = {}
  await catalogStore.fetchSkills()
}

async function install(card: { skill: any; version: VersionOption | null }) {
  const version = card.version
  if (!version) {
    ElMessage.warning('请选择要安装的版本')
    return
  }
  try {
    const result = await skillStore.install({
      skillId: card.skill.id,
      skillName: card.skill.name,
      versionId: version.id,
      versionName: version.name
    })
    ElMessage.success(`已解压到 ${result.path}（${result.fileCount} 个文件）`)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '安装失败')
  }
}

async function remove(item: any) {
  try {
    await ElMessageBox.confirm(`确定移除本地技能「${item.name}」？`, '确认移除', {
      type: 'warning'
    })
  } catch {
    return
  }
  await skillStore.remove(item.name)
  ElMessage.success('已移除')
}

function installedVersion(skillId: number): string {
  return skillStore.find(skillId)?.meta?.versionName ?? ''
}

/** 卡片底部的一句提示：本地装了哪一版、与当前选中的版本是否同一版（装过的版本号会变） */
function installedHint(skill: any, version: VersionOption | null): string {
  const installed = installedVersion(skill.id)
  if (!installed) return ''
  return version && installed === version.name ? '已安装的是这一版' : `本地已安装 v${installed}`
}

/** 选了哪一版：卡片上的「下载并安装」随后就装它 */
function selectVersion(skill: any, version: VersionOption): void {
  picked.value = { ...picked.value, [skill.id]: version }
}

/**
 * 版本标签能不能点：只有一个版本时没什么可切的，与以前的处理一致置灰；
 * 服务端没给 versionCount（旧版本服务端）时不置灰，展开面板自己会去取清单。
 */
function versionSwitchable(skill: any): boolean {
  const count = Number(skill?.versionCount ?? -1)
  return count !== 0 && count !== 1
}
</script>

<template>
  <div class="skill-management">
    <div class="page-header">
      <div>
        <h2>技能管理</h2>
        <p class="page-desc">
          技能包从服务端下载并解压到本地 skills 目录：{{ skillStore.root || '未初始化' }}
        </p>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="技能市场" name="marketplace">
        <div class="marketplace-search">
          <el-input
            v-model="keyword"
            class="search-input"
            placeholder="搜索技能名称或描述"
            clearable
          />
          <el-button :loading="catalogStore.loadingSkills" @click="refreshSkills"> 刷新 </el-button>
          <span class="search-count">共 {{ filtered.length }} 个技能</span>
        </div>
        <el-alert
          v-if="catalogStore.skillError"
          class="catalog-alert"
          type="warning"
          :closable="false"
          show-icon
          :title="catalogStore.skillError"
        >
          <el-button text type="primary" size="small" @click="catalogStore.fetchSkills()">
            重试
          </el-button>
        </el-alert>
        <div v-loading="catalogStore.loadingSkills">
          <el-empty
            v-if="cards.length === 0 && !catalogStore.skillError"
            description="暂无可用技能"
          />
          <div v-else class="skill-grid">
            <el-card v-for="card in cards" :key="card.skill.id" shadow="hover" class="skill-card">
              <template #header>
                <div class="card-header">
                  <span class="card-title" :title="card.skill.name">{{ card.skill.name }}</span>
                  <!-- 版本号：右上角的下拉，点开可检索与切换（公共组件，与项目分享卡片同一套） -->
                  <VersionSelect
                    :loader="card.loader"
                    :current="card.version"
                    :applied="installedVersion(card.skill.id)"
                    :disabled="!versionSwitchable(card.skill)"
                    @select="selectVersion(card.skill, $event)"
                  />
                </div>
              </template>
              <!-- 描述超过两行会截断：悬停看全文 -->
              <p class="card-desc" :title="card.skill.description || ''">
                {{ card.skill.description || '暂无描述' }}
              </p>
              <div class="card-actions">
                <span v-if="card.installed" class="card-meta">{{ card.installed }}</span>
                <el-button
                  text
                  type="primary"
                  :disabled="!card.version"
                  :loading="card.version !== null && skillStore.installing === card.version.id"
                  :title="card.version ? `下载并安装 v${card.version.name}` : '暂无版本可安装'"
                  @click="install(card)"
                >
                  下载并安装
                </el-button>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`已安装 (${skillStore.installed.length})`" name="installed">
        <div v-loading="skillStore.loading">
          <el-empty v-if="skillStore.installed.length === 0" description="暂无已安装技能" />
          <div v-else class="skill-grid">
            <el-card
              v-for="item in skillStore.installed"
              :key="item.path"
              shadow="hover"
              class="skill-card"
            >
              <template #header>
                <div class="card-header">
                  <span class="card-title">{{ item.name }}</span>
                  <el-tag size="small">v{{ item.meta?.versionName || '-' }}</el-tag>
                </div>
              </template>
              <p class="card-desc">
                {{ item.fileCount }} 个文件 · {{ (item.size / 1024).toFixed(1) }} KB
              </p>
              <p class="card-path">{{ item.path }}</p>
              <div class="card-actions">
                <el-button text type="danger" @click="remove(item)">移除</el-button>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style lang="scss" scoped>
.skill-management {
  padding: 24px 32px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h2 {
    font-size: 20px;
    color: var(--ev-c-text-1);
  }

  .page-desc {
    margin-top: 6px;
    font-size: 13px;
    color: var(--ev-c-text-3);
  }
}

.marketplace-search {
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

.catalog-alert {
  margin-bottom: 12px;
}

.skill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.skill-card {
  display: flex;
  flex-direction: column;
  transition: border-color 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
  }

  :deep(.el-card__header) {
    padding: 12px 16px;
  }

  :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 14px 16px 12px;
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .card-title {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
    font-weight: 600;
    color: var(--ev-c-text-1);
  }

  .card-desc {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin: 0 0 12px;
    font-size: 13px;
    line-height: 1.6;
    color: var(--ev-c-text-3);
  }

  .card-path {
    margin: 0 0 12px;
    padding: 6px 8px;
    font-size: 12px;
    line-height: 1.5;
    color: var(--ev-c-text-3);
    background: var(--el-fill-color-lighter);
    border-radius: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
    margin-top: auto;
  }

  /* 本地已安装的版本提示：占左边，安装按钮留在右边 */
  .card-meta {
    margin-right: auto;
    font-size: 12px;
    color: var(--ev-c-text-3);
  }
}
</style>
