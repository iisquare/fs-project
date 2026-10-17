<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useProjectStore } from '@renderer/stores/project'
import { useShareStore } from '@renderer/stores/share'
import { errorMessage } from '@renderer/utils/ErrorUtil'
import type { Project } from '@renderer/db/ProjectDb'

/**
 * 分享表单：把某个本地项目的配置发布到服务端（在项目管理页的「我的分享」上下文里打开）。
 *
 * 分享目标是「新建分享项目」或自己历史分享过的某个项目：
 * 选已有的项目就是在它下面追加一个版本，填版本号与版本说明即可发布。
 * 面板外壳（返回条）由页面提供，这里只负责表单本身。
 */

const props = defineProps<{
  /** 预选的本地项目：从项目编辑器点「分享」进来时带上（也可只给 target，由表单反查发布过它的本地项目） */
  projectId?: string
  /** 预选的分享目标：从「我的分享」的卡片点「分享新版本」进来时带上 */
  target?: number
}>()

const emit = defineEmits<{
  published: []
  cancel: []
}>()

const projectStore = useProjectStore()
const shareStore = useShareStore()

const form = ref({ projectId: '', target: 0, name: '', version: '', versionDescription: '' })
const saving = ref(false)
const loading = ref(false)
const loadError = ref('')
/** 当前选中的本地项目：提示「还没配模型」这类会跟着别人一起带过去的问题 */
const sharingProject = computed(() => projectStore.find(form.value.projectId))

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    if (!projectStore.projects.length) await projectStore.fetchProjects()
    if (!shareStore.mine.length) await shareStore.fetchMine()
    preset()
  } catch (error) {
    loadError.value = errorMessage(error, '分享表单数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})

/** 版本号建议：最近版本是纯数字（1 → 2）或两段数字（1.0 → 1.1）时顺延一位，其余情况留空由用户填 */
function nextVersion(latest: string): string {
  const text = String(latest ?? '').trim()
  const plain = /^(\d+)$/.exec(text)
  if (plain) return String(Number(plain[1]) + 1)
  const dotted = /^(\d+)\.(\d+)$/.exec(text)
  if (dotted) return `${dotted[1]}.${Number(dotted[2]) + 1}`
  return ''
}

/** 本地项目：优先用页面指定的项目，其次按「分享目标」反查发布过它的本地项目 */
function resolveProject(): Project | null {
  if (props.projectId) {
    const found = projectStore.find(props.projectId)
    if (found) return found
  }
  const target = Number(props.target ?? 0)
  if (target) {
    return projectStore.projects.find((item) => Number(item.shareId) === target) ?? null
  }
  return null
}

/** 按预选信息铺开表单：项目 → 分享目标（项目记着的那一个）→ 名称与建议版本号 */
function preset(): void {
  const project = resolveProject()
  const target = Number(props.target ?? 0) || Number(project?.shareId ?? 0)
  const targetShare = target ? shareStore.findMine(target) : null
  form.value = {
    projectId: project?.id ?? '',
    target,
    name: targetShare?.name ?? project?.name ?? '',
    version: nextVersion(targetShare?.latestVersion?.name ?? ''),
    versionDescription: ''
  }
}

/** 换本地项目：把分享目标与名称同步成这个项目的分享去向（用户随后仍可改） */
function syncFromProject(): void {
  const project = projectStore.find(form.value.projectId)
  const target = Number(project?.shareId ?? 0)
  const targetShare = target ? shareStore.findMine(target) : null
  form.value.target = target
  form.value.name = targetShare?.name ?? project?.name ?? ''
  form.value.version = nextVersion(targetShare?.latestVersion?.name ?? '')
}

/** 当前分享目标最近一次发布的版本号：填版本号时的参考 */
function targetLatest(): string {
  const target = Number(form.value.target ?? 0)
  if (!target) return ''
  return shareStore.findMine(target)?.latestVersion?.name ?? ''
}

async function submit(): Promise<void> {
  const project = projectStore.find(form.value.projectId)
  if (!project) {
    ElMessage.warning('请选择要分享的本地项目')
    return
  }
  if (!form.value.target && !form.value.name.trim()) {
    ElMessage.warning('新建分享项目时请填写分享名称')
    return
  }
  if (!form.value.version.trim()) {
    ElMessage.warning('请填写版本号')
    return
  }
  saving.value = true
  try {
    const data = await shareStore.publish({
      project,
      projectId: Number(form.value.target ?? 0),
      name: form.value.name.trim(),
      version: form.value.version.trim(),
      versionDescription: form.value.versionDescription
    })
    const refreshes = await Promise.allSettled([
      projectStore.fetchProjects(),
      shareStore.fetchMine()
    ])
    const refreshFailure = refreshes.find(
      (item): item is PromiseRejectedResult => 'rejected' === item.status
    )
    ElMessage.success(`已发布到「${data?.project?.name ?? ''}」v${data?.version?.name ?? ''}`)
    if (refreshFailure) {
      ElMessage.warning(errorMessage(refreshFailure.reason, '发布成功，但列表刷新失败'))
    }
    emit('published')
  } catch (error) {
    ElMessage.error(errorMessage(error, '发布失败'))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-form label-width="96px" class="share-form" v-loading="loading">
    <el-alert
      v-if="loadError"
      class="share-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="loadError"
    >
      <el-button text type="primary" size="small" @click="load">重试</el-button>
    </el-alert>
    <el-form-item label="本地项目">
      <el-select
        v-model="form.projectId"
        placeholder="选择要分享的项目"
        style="width: 100%"
        filterable
        @change="syncFromProject"
      >
        <el-option
          v-for="item in projectStore.projects"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
      <div class="field-tip">
        分享的是这个项目的当前配置：模型、上下文大小、系统提示词、记忆、思考强度、工具、环境变量（标了「密钥」的除外）与工作区目录。
      </div>
      <div class="field-tip" v-if="sharingProject && !sharingProject.model">
        这个项目还没配置模型：别人取用这份配置后要自己补一个模型才能跑起来。
      </div>
    </el-form-item>
    <el-form-item label="分享目标">
      <el-select v-model="form.target" style="width: 100%" filterable>
        <el-option label="新建分享项目" :value="0" />
        <el-option
          v-for="item in shareStore.mine"
          :key="item.id"
          :label="`${item.name}（已有 ${item.versionCount} 个版本）`"
          :value="item.id"
        />
      </el-select>
      <div class="field-tip">
        选择已有的分享项目就是在它下面追加一个版本；新建会在服务端建一个新的分享项目。
      </div>
      <div class="field-tip" v-if="shareStore.mineTotal > shareStore.mine.length">
        这里只列出了已载入的 {{ shareStore.mine.length }} 个（共 {{ shareStore.mineTotal }} 个）：
        在「我的分享」里点「加载更多」后回来，剩下的分享项目也会出现在这里。
      </div>
    </el-form-item>
    <el-form-item label="分享名称">
      <el-input v-model="form.name" placeholder="项目的展示名称" />
      <div class="field-tip">
        新建分享项目时以它命名；选择已有项目时会同步成这个名称，留空保持原名称。
      </div>
    </el-form-item>
    <el-form-item label="版本号">
      <el-input v-model="form.version" placeholder="如 1.0 / 2026-10-14" />
      <div class="field-tip" v-if="targetLatest()">
        该分享项目最近一次发布的版本是 v{{ targetLatest() }}，已按它给出建议版本号。
      </div>
      <div class="field-tip" v-else>版本号由你决定，用于在版本记录里区分这次发布。</div>
    </el-form-item>
    <el-form-item label="版本说明">
      <el-input
        v-model="form.versionDescription"
        type="textarea"
        :rows="3"
        placeholder="这一版改了什么，供自己与使用者对照"
      />
    </el-form-item>
    <el-alert
      class="share-alert"
      type="warning"
      :closable="false"
      show-icon
      title="分享内容会包含非密钥的环境变量、记忆与工作区目录路径，请确认后再发布；标了「密钥」的环境变量只留在本机"
    />
    <div class="editor-footer">
      <el-button @click="emit('cancel')">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">发布</el-button>
    </div>
  </el-form>
</template>

<style lang="scss" scoped>
.share-form {
  padding: 24px 24px 0;
}

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ev-c-text-3);
}

.share-alert {
  margin-bottom: 12px;
}

.editor-footer {
  position: sticky;
  bottom: 0;
  z-index: 2;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 18px;
  margin: 0 -24px;
  background: color-mix(in srgb, var(--ui-surface) 92%, transparent);
  border: 0;
  border-top: 1px solid var(--ui-divider);
  border-radius: 0 0 var(--ui-card-radius) var(--ui-card-radius);
  box-shadow: none;
  backdrop-filter: blur(14px);
}
</style>
