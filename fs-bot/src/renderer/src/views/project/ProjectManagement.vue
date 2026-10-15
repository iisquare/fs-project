<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useProjectStore } from '@renderer/stores/project'
import { useShareStore } from '@renderer/stores/share'
import { useConversationStore } from '@renderer/stores/conversation'
import { page, projectCreateAction } from '@renderer/router/config'
import ApiUtil from '@renderer/utils/ApiUtil'
import { parseProjectFile, uniqueName } from '@renderer/utils/ProjectUtil'
import ProjectEditor from './ProjectEditor.vue'
import ShareList from '@renderer/components/Project/ShareList.vue'
import ProjectShareForm from '@renderer/components/Project/ProjectShareForm.vue'
import type { Project } from '@renderer/db/ProjectDb'
import type { ShareProject } from '@renderer/stores/share'

/**
 * 项目管理：列表与表单是两个独立视图（表单替换列表、占满内容区），列表里再分三个页签。
 *
 * - 我的项目：本地项目与它们的分享去向（卡片上回显「已分享 v1.0」），可导出 / 导入配置、新建与编辑；
 * - 我的分享：自己发布到服务端的项目与版本记录，可以继续追加版本、导出任一版本的配置；
 * - 浏览分享：其他用户分享的项目，可以导出配置或直接创建成本地项目。
 *
 * 分享的列表与表单分别收在 components/Project/ShareList.vue 与 ProjectShareForm.vue，
 * 页面只负责页签、视图切换与页头动作。项目没有「当前」一说：会话按项目创建，
 * 每个会话用自己的项目口径，所以这里的操作只有「在这个项目里开新会话」「编辑」「删除」。
 */

const router = useRouter()
const route = useRoute()
const projectStore = useProjectStore()
const shareStore = useShareStore()

const activeTab = ref<'project' | 'mine' | 'browse'>('project')
const mode = ref<'list' | 'project' | 'share'>('list')
const editing = ref<Project | null>(null)
/** 分享表单的预选：来自项目编辑器的「分享」或「我的分享」卡片上的「分享新版本」 */
const sharePreset = ref<{ projectId: string; target: number }>({ projectId: '', target: 0 })

onMounted(async () => {
  await projectStore.fetchProjects()
  // 两个分享列表的可见范围都要问服务端，页签上的数量也得等它回来：进页面先各取一次
  await Promise.all([shareStore.fetchMine(), shareStore.fetchBrowse()])
  // 从「暂无项目」的空状态进来时（?action=create）直接展开新建表单，
  // 不用先落到空列表再点一次「新建项目」；随后清掉参数，刷新或返回不再重进表单
  if (projectCreateAction === route.query.action) {
    openCreate()
    router.replace({ path: page.project })
  }
})

function openCreate() {
  mode.value = 'project'
  editing.value = null
}

function selectProject(project: Project) {
  mode.value = 'project'
  editing.value = { ...project }
}

/** 回到列表：未保存的改动丢弃 */
function backToList(): void {
  mode.value = 'list'
  editing.value = null
}

/** 保存后按 id 回到刚存下的那条，继续停留在这个项目的表单里 */
function onSaved(id: string) {
  if (!id) return
  const project = projectStore.find(id)
  if (!project) return
  mode.value = 'project'
  editing.value = { ...project }
}

/** 取消：丢弃未保存的改动，回到列表 */
function onCancel() {
  backToList()
}

async function remove(project: Project) {
  try {
    await ElMessageBox.confirm(`确定删除项目「${project.name}」？`, '确认删除', { type: 'warning' })
  } catch {
    return
  }
  await projectStore.remove(project.id)
  if (editing.value?.id === project.id) {
    backToList()
  }
}

/** 以某个项目直接开新会话 */
async function startChat(project: Project): Promise<void> {
  const conversation = await useConversationStore().create(project)
  await useConversationStore().select(conversation.id)
  router.push(page.home)
}

/**
 * 导入项目配置：主进程弹选择框读回文件原文，这边解析成项目配置后建成本地项目。
 * 重名时自动加「（导入）」后缀；文件来自分享时把来源写进提示，便于确认导入的是什么。
 */
async function importProject(): Promise<void> {
  try {
    const file = await window.ipc.project.importFile()
    if (!file.path) return
    const { project, share } = parseProjectFile(file.content)
    project.name = uniqueName(
      project.name,
      projectStore.projects.map((item) => item.name)
    )
    const result: any = await projectStore.create(project)
    const id = String(ApiUtil.data(result)?.id ?? '')
    const source = share?.projectName
      ? `（来自分享「${share.projectName}」v${share.versionName || '-'}）`
      : ''
    ElMessage.success(`已导入项目「${project.name}」${source}`)
    activeTab.value = 'project'
    if (id) {
      const created = projectStore.find(id)
      if (created) selectProject(created)
    }
  } catch (error) {
    ElMessage.error(`导入失败：${error instanceof Error ? error.message : String(error)}`)
  }
}

/**
 * 打开分享表单：从项目编辑器进来时带项目标识（分享的就是这个项目），
 * 从「我的分享」的卡片进来时带分享目标（在那个项目下追加版本）。
 */
function openShareForm(preset: { projectId?: string; target?: number } = {}): void {
  sharePreset.value = { projectId: preset.projectId ?? '', target: preset.target ?? 0 }
  activeTab.value = 'mine'
  mode.value = 'share'
}

/** 发布完成：回到「我的分享」，列表与本地项目都由表单那边刷过 */
function onPublished(): void {
  mode.value = 'list'
  activeTab.value = 'mine'
}

/** 「我的分享」某一项要追加版本 */
function shareNewVersion(share: ShareProject): void {
  openShareForm({ target: share.id })
}
</script>

<template>
  <div class="project-management">
    <!-- 只有列表视图有页头；表单视图不再重复一个标题，名称就在表单里 -->
    <div v-if="mode === 'list'" class="page-header">
      <div>
        <h2>项目管理</h2>
        <p class="page-desc">
          项目选用模型，并在此配置环境变量、工具与工作区；编排应用作为一种工具被调用，新会话按项目创建。
          项目配置可以导出 / 导入，也可以发布到服务端分享：「我的分享」管理自己发布的项目与版本，
          「浏览分享」里可以取用别人分享的项目。
        </p>
      </div>
      <div class="header-actions">
        <template v-if="activeTab === 'project'">
          <el-button @click="importProject">导入项目</el-button>
          <el-button type="primary" @click="openCreate">新建项目</el-button>
        </template>
        <el-button v-else-if="activeTab === 'mine'" type="primary" @click="openShareForm()">
          分享项目
        </el-button>
      </div>
    </div>

    <!-- 列表视图：三个页签共用这块内容区 -->
    <div v-if="mode === 'list'">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="`我的项目（${projectStore.projects.length}）`" name="project">
          <div v-loading="projectStore.loading">
            <el-empty v-if="projectStore.projects.length === 0" description="暂无项目">
              <el-button type="primary" @click="openCreate">新建项目</el-button>
            </el-empty>
            <div v-else class="project-grid">
              <el-card
                v-for="item in projectStore.projects"
                :key="item.id"
                shadow="hover"
                class="project-card"
              >
                <div class="card-top">
                  <el-avatar :size="44" :src="item.icon">{{ item.name.charAt(0) }}</el-avatar>
                  <div class="card-title">
                    <span class="name" :title="item.name">{{ item.name }}</span>
                    <!-- 模型与分享去向同一行：模型名可能很长，让它自己收口；分享标记完整展示 -->
                    <div class="tag-row">
                      <!-- 模型只在这里出现一次：卡片下面的统计行不再重复它 -->
                      <el-tag v-if="item.model" class="model-tag" size="small" :title="item.model">
                        {{ item.model }}
                      </el-tag>
                      <el-tag v-else class="model-tag" size="small" type="warning"
                        >未配置模型</el-tag
                      >
                      <el-tag
                        v-if="item.shareId"
                        class="share-tag"
                        size="small"
                        type="success"
                        :title="`已分享到「${item.shareName || '分享项目'}」`"
                      >
                        已分享 v{{ item.shareVersion || '-' }}
                      </el-tag>
                    </div>
                  </div>
                </div>
                <!-- 描述超过两行会截断：悬停看全文 -->
                <p class="card-desc" :title="item.description || ''">
                  {{ item.description || '暂无描述' }}
                </p>
                <!-- 数量与操作同一行：左边计数，右边按钮 -->
                <div class="card-actions">
                  <div class="card-meta">
                    <span>工具 {{ item.tools.length }}</span>
                    <span>工作区 {{ item.workspace.length }}</span>
                  </div>
                  <el-button text type="primary" :disabled="!item.model" @click="startChat(item)">
                    新建会话
                  </el-button>
                  <el-button text type="primary" @click="selectProject(item)">编辑</el-button>
                  <el-button text type="danger" @click="remove(item)">删除</el-button>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`我的分享（${shareStore.mineTotal}）`" name="mine">
          <ShareList mode="mine" @new-version="shareNewVersion" />
        </el-tab-pane>

        <el-tab-pane :label="`浏览分享（${shareStore.browseTotal}）`" name="browse">
          <ShareList mode="browse" @created="activeTab = 'project'" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 项目表单视图：同样占满内容区 -->
    <section v-else-if="mode === 'project'" class="editor-pane">
      <div class="editor-bar">
        <el-button text class="back-button" @click="backToList">← 返回项目列表</el-button>
        <div v-if="editing" class="bar-actions">
          <el-tag v-if="!editing.model" size="small" type="warning"> 未配置模型 </el-tag>
          <el-button text type="primary" :disabled="!editing.model" @click="startChat(editing)">
            新建会话
          </el-button>
          <el-button text type="danger" @click="remove(editing)">删除</el-button>
        </div>
      </div>

      <ProjectEditor
        :key="editing?.id ?? 'create'"
        :project="editing"
        :creating="!editing"
        @saved="onSaved"
        @cancel="onCancel"
        @share="(id: string) => openShareForm({ projectId: id })"
      />
    </section>

    <!-- 分享表单视图：与项目表单同一套外壳，返回后停在「我的分享」 -->
    <section v-else class="editor-pane">
      <div class="editor-bar">
        <el-button text class="back-button" @click="backToList">← 返回项目列表</el-button>
      </div>
      <ProjectShareForm
        :key="`${sharePreset.projectId}-${sharePreset.target}`"
        :project-id="sharePreset.projectId"
        :target="sharePreset.target"
        @published="onPublished"
        @cancel="backToList"
      />
    </section>
  </div>
</template>

<style lang="scss" scoped>
.project-management {
  padding: 24px 32px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;

  h2 {
    font-size: 20px;
    color: var(--ev-c-text-1);
  }

  .page-desc {
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.7;
    color: var(--ev-c-text-3);
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.project-card {
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

  .card-top {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .card-title {
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

    /* 模型与分享标记同一行：窄窗口放不下时分享标记换到下一行，不被压扁 */
    .tag-row {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 6px;
      min-width: 0;
      max-width: 100%;
    }
  }

  /* 模型名可能很长：标签自身收口（完整名字在 title 里），并让它在同行里可以收缩 */
  .model-tag {
    min-width: 0;
    max-width: 100%;
    overflow: hidden;

    :deep(.el-tag__content) {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  /* 分享标记短且固定：不参与收缩，模型名再长也完整显示 */
  .share-tag {
    flex: none;
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

  .card-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: var(--ev-c-text-3);
    /* 同一行里占左边，按钮留在右边 */
    margin-right: auto;
  }

  .card-actions {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    margin-top: auto;
    padding-top: 12px;
  }
}

.editor-pane {
  min-height: 360px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.editor-bar {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  border-radius: 8px 8px 0 0;
}

.back-button {
  padding: 0;
  font-size: 13px;
  color: var(--ev-c-text-3);

  &:hover {
    color: var(--el-color-primary);
    background: transparent;
  }
}

.bar-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
</style>
