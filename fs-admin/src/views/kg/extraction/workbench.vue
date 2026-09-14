<script setup lang="ts">
/**
 * 知识抽取工作台
 *
 * 1. 左侧数据源列表：支持名称检索、处理状态筛选、排序与分页；点击某条只用于查看信息；
 * 2. 新增 / 编辑数据源统一走弹窗；
 * 3. 中间展示所选数据源的文本内容，内容下方展示录入人、录入时间、处理人、处理时间、处理状态等详细信息；
 * 4. 顶部配置本体与置信度阈值，点「开始抽取」或打开「自动预览」即时查看识别结果；
 * 5. 右侧按实体 / 关系 / 属性审核并逐条采纳；
 * 6. 底部「入图」把已采纳的候选按主键 MERGE 写入图数据库，并把该数据源标记为已处理。
 *
 * 抽取只做预览、不落库；只有入图才会写图数据库，且仅写入人工采纳的内容。
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import DateUtil from '@/utils/DateUtil'
import ExtractApi from '@/api/kg/ExtractApi'
import GraphApi from '@/api/kg/GraphApi'
import OntologyApi from '@/api/kg/OntologyApi'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'

const ontologies = ref<any[]>([])
const entities = ref<any[]>([])
const relationships = ref<any[]>([])
const ontologyId = ref<any>(null)
const mode = ref('RULE')
const threshold = ref(0.7)
const autoPreview = ref(false)

const sources = ref<any[]>([])
const listQuery = ref<any>({ keyword: '', processStatus: '', sort: 'createdTime' })
const pagination = ref<any>({ page: 1, pageSize: 15, total: 0 })
const detail = ref<any>(null)
const marking = ref(false)
const showDetail = ref(true)

const dialogVisible = ref(false)
const dialogTitle = ref('新增数据源')
const saving = ref(false)
const sourceForm = ref<any>({ id: 0, name: '', type: 'TEXT', filename: '', content: '' })

const extracting = ref(false)
const applying = ref(false)
const preview = ref<any>({ rows: [], entityCount: 0, relationshipCount: 0, attributeCount: 0, pendingCount: 0 })
const activeKind = ref('ENTITY')
const accepted = ref<any>({})
let previewTimer: any = null

const KIND_LIST = [
  { value: 'ENTITY', label: '实体' },
  { value: 'RELATIONSHIP', label: '关系' },
  { value: 'ATTRIBUTE', label: '属性' },
]

const statusText = (value: any) => (Number(value ?? 0) > 0 ? '已处理' : '未处理')
const kindCount = (kind: string) => {
  if ('ENTITY' === kind) return preview.value.entityCount ?? 0
  if ('RELATIONSHIP' === kind) return preview.value.relationshipCount ?? 0
  return preview.value.attributeCount ?? 0
}
const rows = computed(() => (preview.value.rows ?? []).filter((item: any) => item.kind === activeKind.value))
const rowKey = (item: any, index: number) => `${item.kind}-${item.label}-${item.primaryValue}-${index}`
const acceptedCount = computed(() =>
  (preview.value.rows ?? []).filter((item: any, index: number) => accepted.value[rowKey(item, index)]).length)

/* ---------------- 本体 ---------------- */

const handleOntologies = () => {
  OntologyApi.list({ page: 1, pageSize: 200 }).then((result: any) => {
    ontologies.value = ApiUtil.data(result)?.rows ?? []
    if (!ontologyId.value && ontologies.value.length) {
      ontologyId.value = ontologies.value[0].id
      handleModel()
    }
  }).catch(() => {})
}

const handleModel = () => {
  entities.value = []
  relationships.value = []
  if (!ontologyId.value) return
  OntologyApi.model({ id: ontologyId.value }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    entities.value = data.entities ?? []
    relationships.value = data.relationships ?? []
  }).catch(() => {})
}

/* ---------------- 数据源列表 ---------------- */

const handleSources = () => {
  ExtractApi.sourceList({
    keyword: listQuery.value.keyword || undefined,
    processStatus: '' === listQuery.value.processStatus ? undefined : listQuery.value.processStatus,
    sort: listQuery.value.sort,
    page: pagination.value.page,
    pageSize: pagination.value.pageSize,
  }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    sources.value = data.rows ?? []
    pagination.value.total = data.total ?? 0
  }).catch(() => {})
}

const handleSearch = () => {
  pagination.value.page = 1
  handleSources()
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  handleSources()
}

const handleSizeChange = (size: number) => {
  pagination.value.pageSize = size
  pagination.value.page = 1
  handleSources()
}

/**
 * 点击列表只用于查看信息：加载详情、清空上一次抽取结果
 */
const handleSelectSource = (item: any) => {
  ExtractApi.sourceInfo({ id: item.id }, { warning: false }).then((result: any) => {
    detail.value = ApiUtil.data(result) ?? null
    showDetail.value = true
    resetPreview()
    if (autoPreview.value) handlePreview()
  }).catch((result: any) => ElMessage.warning(ApiUtil.message(result)))
}

const handleDeleteSource = (item: any) => {
  TableUtil.confirm(`确认删除数据源[${item.name}]？`, '删除确认').then(() => {
    ExtractApi.sourceDelete({ ids: [item.id] }, { success: true }).then(() => {
      if (detail.value?.id === item.id) detail.value = null
      handleSources()
    }).catch(() => {})
  }).catch(() => {})
}

/**
 * 标记处理状态：入图后由系统标记，也可以在详情里手工调整
 */
const handleMark = (status: number) => {
  if (!detail.value) return
  marking.value = true
  ExtractApi.sourceMark({ id: detail.value.id, processStatus: status }, { success: true }).then(() => {
    handleSelectSource({ id: detail.value.id })
    handleSources()
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    marking.value = false
  })
}

/* ---------------- 数据源弹窗 ---------------- */

const handleAddSource = () => {
  dialogTitle.value = '新增数据源'
  sourceForm.value = { id: 0, name: '', type: 'TEXT', filename: '', content: '' }
  dialogVisible.value = true
}

const handleEditSource = (row: any) => {
  const target = row ?? detail.value
  if (!target) return
  ExtractApi.sourceInfo({ id: target.id }, { warning: false }).then((result: any) => {
    dialogTitle.value = '编辑数据源'
    sourceForm.value = Object.assign({ id: 0, name: '', type: 'TEXT', filename: '', content: '' }, ApiUtil.data(result) ?? {})
    dialogVisible.value = true
  }).catch((result: any) => ElMessage.warning(ApiUtil.message(result)))
}

/**
 * 上传文本文件：txt / csv 直接读取，xlsx 交给后端解析后拼接为文本
 */
const handleUpload = (file: any) => {
  const name = String(file.name ?? '')
  sourceForm.value.name = sourceForm.value.name || name.replace(/\.[^.]+$/, '')
  sourceForm.value.filename = name
  sourceForm.value.type = 'FILE'
  if (/\.xlsx$/i.test(name)) {
    GraphApi.importExcel({ file: file.raw }, { warning: false }).then((result: any) => {
      const lines: any[] = ApiUtil.data(result) ?? []
      sourceForm.value.content = lines.map((line: any[]) => (line ?? []).join('\t')).join('\n')
      ElMessage.success(`已解析 ${lines.length} 行`)
    }).catch((result: any) => ElMessage.warning(ApiUtil.message(result)))
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    sourceForm.value.content = String(reader.result ?? '')
    ElMessage.success('已读取文件内容')
  }
  reader.readAsText(file.raw, 'utf-8')
}

const handleSubmitSource = () => {
  if (!sourceForm.value.name) return ElMessage.warning('请填写数据源名称')
  if (!sourceForm.value.content || !sourceForm.value.content.trim()) return ElMessage.warning('数据源内容不能为空')
  saving.value = true
  ExtractApi.sourceSave(sourceForm.value, { success: true }).then((result: any) => {
    dialogVisible.value = false
    const id = ApiUtil.data(result)?.id ?? sourceForm.value.id
    handleSources()
    handleSelectSource({ id })
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    saving.value = false
  })
}

/* ---------------- 抽取预览 ---------------- */

const resetPreview = () => {
  preview.value = { rows: [], entityCount: 0, relationshipCount: 0, attributeCount: 0, pendingCount: 0 }
  accepted.value = {}
  activeKind.value = 'ENTITY'
}

/**
 * 开始抽取：直接使用当前数据源的文本，结果只在前端保存
 */
const handlePreview = () => {
  if (!ontologyId.value) return ElMessage.warning('请先选择本体')
  if ('LLM' === mode.value) return ElMessage.warning('大模型抽取需要先接入模型服务，当前请使用规则词典方式')
  if (!detail.value) return ElMessage.warning('请先在左侧选择数据源')
  const content = detail.value.content ?? ''
  if (!content.trim()) return ElMessage.warning('该数据源内容为空')
  extracting.value = true
  ExtractApi.preview({
    ontologyId: ontologyId.value,
    threshold: threshold.value,
    content,
  }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? { rows: [] }
    preview.value = data
    accepted.value = {}
    ;(data.rows ?? []).forEach((item: any, index: number) => {
      accepted.value[rowKey(item, index)] = item.status === 2
    })
    activeKind.value = (data.entityCount ?? 0) > 0 ? 'ENTITY'
      : ((data.relationshipCount ?? 0) > 0 ? 'RELATIONSHIP' : 'ATTRIBUTE')
    if (!(data.rows ?? []).length) {
      ElMessage.info('没有识别到内容：请确认本体已定义实体字段，且图数据或扩展词典中存在对应词条')
    }
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    extracting.value = false
  })
}

const schedulePreview = () => {
  if (!autoPreview.value) return
  if (previewTimer) clearTimeout(previewTimer)
  previewTimer = setTimeout(() => handlePreview(), 1200)
}

const handleToggleAll = (checked: boolean) => {
  rows.value.forEach((item: any) => {
    const index = (preview.value.rows ?? []).indexOf(item)
    accepted.value[rowKey(item, index)] = checked
  })
}

/* ---------------- 正文标记 ---------------- */

const markKey = ref('')
const MARK_PALETTE = ['#409eff', '#67c23a', '#e6a23c', '#9b59b6', '#16a085', '#d35400', '#f56c6c', '#909399']

const ESCAPE_MAP: any = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }
const escapeHtml = (text: string) => String(text).replace(/[&<>"']/g, (char: string) => ESCAPE_MAP[char] ?? char)

/**
 * 实体配色：优先取本体里为该实体配置的颜色，未配置时按顺序分配
 */
const entityColor = (label: string) => {
  const def = entities.value.find((item: any) => item.label === label)
  if (def?.color) return def.color
  const index = entities.value.findIndex((item: any) => item.label === label)
  return MARK_PALETTE[(index < 0 ? 0 : index) % MARK_PALETTE.length]
}

/**
 * 正文标记：把识别到的实体按原文位置高亮，未采纳的置灰
 *
 * 位置来自抽取结果的 start/end（相对正文的字符偏移），重叠时保留先出现的片段。
 */
const markedHtml = computed(() => {
  const text = detail.value?.content ?? ''
  if (!text) return ''
  const marks = (preview.value.rows ?? [])
    .map((item: any, index: number) => ({
      key: rowKey(item, index),
      label: item.label,
      confidence: item.confidence,
      start: Number(item.payload?.start ?? -1),
      end: Number(item.payload?.end ?? -1),
    }))
    .filter((item: any) => item.start >= 0 && item.end > item.start && item.end <= text.length)
    .sort((a: any, b: any) => a.start - b.start || (b.end - b.start) - (a.end - a.start))
  if (!marks.length) return escapeHtml(text)
  const parts: string[] = []
  let cursor = 0
  let lastEnd = 0
  marks.forEach((item: any) => {
    if (item.start < lastEnd) return
    parts.push(escapeHtml(text.slice(cursor, item.start)))
    const acceptedMark = accepted.value[item.key]
    const active = item.key === markKey.value ? ' wb-mark--active' : ''
    const ignored = acceptedMark ? '' : ' wb-mark--ignored'
    parts.push(`<span class="wb-mark${ignored}${active}" data-key="${item.key}"`
      + ` style="--mark-color:${entityColor(item.label)}"`
      + ` title="${escapeHtml(entityName(item.label))} · 置信度 ${Math.round(Number(item.confidence ?? 0) * 100)}%`
      + `${acceptedMark ? ' · 已采纳' : ' · 未采纳'}">${escapeHtml(text.slice(item.start, item.end))}</span>`)
    cursor = item.end
    lastEnd = item.end
  })
  parts.push(escapeHtml(text.slice(cursor)))
  return parts.join('')
})

const markOf = (event: any) => {
  const node = (event.target as HTMLElement)?.closest?.('[data-key]')
  return node ? String(node.getAttribute('data-key') ?? '') : ''
}

const handleMarkEnter = (event: any) => {
  const key = markOf(event)
  if (key) markKey.value = key
}

const handleMarkLeave = () => {
  markKey.value = ''
}

/**
 * 点击正文中的标记，定位到右侧对应候选
 */
const handleMarkClick = (event: any) => {
  const key = markOf(event)
  if (!key) return
  activeKind.value = key.startsWith('RELATIONSHIP') || key.startsWith('ATTRIBUTE') ? key.split('-')[0] : 'ENTITY'
  markKey.value = key
  nextTick(() => {
    const target = document.querySelector(`.wb-item[data-key="${key}"]`)
    target?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  })
}

/* ---------------- 入图 ---------------- */

/**
 * 候选的展示名称：尽量用中文
 *
 * 实体用命中的原文，关系用本体里的关系名称，属性用字段显示名。
 */
const entityName = (label: string) => entities.value.find((item: any) => item.label === label)?.name || label
const relationshipName = (label: string) => relationships.value.find((item: any) => item.label === label)?.name || label
const primaryFieldOf = (label: string) => entities.value.find((item: any) => item.label === label)?.primaryField || ''

const itemTitle = (row: any) => {
  const payload = row.payload ?? {}
  if ('ENTITY' === row.kind) return payload.text || row.primaryValue || row.label
  if ('RELATIONSHIP' === row.kind) return relationshipName(row.label)
  return payload.title || payload.field || row.label
}

/**
 * 候选描述：补充所属实体、主键与两端等关键信息
 */
const withLabel = (name: string, label: string) => (name === label ? label : `${name}（${label}）`)

const itemDetail = (row: any) => {
  const payload = row.payload ?? {}
  if ('ENTITY' === row.kind) {
    return `${withLabel(entityName(row.label), row.label)}`
      + ` · 主键 ${primaryFieldOf(row.label) || 'code'}：${row.primaryValue || '-'}`
  }
  if ('RELATIONSHIP' === row.kind) {
    const source = payload.sourceText || payload.source || '-'
    const target = payload.targetText || payload.target || '-'
    const head = withLabel(relationshipName(row.label), row.label)
    return `${head} · ${source}（${payload.source ?? '-'}） → ${target}（${payload.target ?? '-'}）`
  }
  return `${withLabel(entityName(row.label), row.label)}`
    + ` · 主键 ${primaryFieldOf(row.label) || 'code'}：${row.primaryValue || '-'}`
    + ` · ${payload.title || payload.field}：${payload.value}`
}

/**
 * 组装入图数据：已采纳的属性并入同主键实体的属性里
 */
const buildApplyPayload = () => {
  const all = preview.value.rows ?? []
  const entityMap: any = {}
  const entityList: any[] = []
  const relationshipList: any[] = []
  const attributeMap: any = {}
  all.forEach((item: any, index: number) => {
    if (!accepted.value[rowKey(item, index)]) return
    if ('ATTRIBUTE' === item.kind) {
      const key = `${item.label}|${item.primaryValue}`
      attributeMap[key] = Object.assign({}, attributeMap[key], { [item.payload?.field]: item.payload?.value })
      return
    }
    if ('ENTITY' === item.kind) {
      const key = `${item.label}|${item.primaryValue}`
      if (!entityMap[key]) {
        entityMap[key] = { label: item.label, properties: Object.assign({}, item.payload?.properties ?? {}) }
        entityList.push(entityMap[key])
      }
      return
    }
    if ('RELATIONSHIP' === item.kind) {
      relationshipList.push({
        label: item.label,
        source: item.payload?.source,
        target: item.payload?.target,
        properties: item.payload?.properties ?? {},
      })
    }
  })
  entityList.forEach((item: any) => {
    const primaryField = entities.value.find((row: any) => row.label === item.label)?.primaryField ?? ''
    const values = attributeMap[`${item.label}|${item.properties?.[primaryField] ?? ''}`]
    if (values) Object.assign(item.properties, values)
  })
  return { ontologyId: ontologyId.value, entities: entityList, relationships: relationshipList }
}

const handleApply = () => {
  if ('LLM' === mode.value) return ElMessage.warning('当前抽取方式不支持入图')
  const payload = buildApplyPayload()
  if (!payload.entities.length && !payload.relationships.length) {
    return ElMessage.warning('没有已采纳的候选，请先执行抽取并采纳')
  }
  TableUtil.confirm(
    `将把实体 ${payload.entities.length} 个、关系 ${payload.relationships.length} 条按主键 MERGE 写入图数据库，`
    + '重复数据自动合并，并把该数据源标记为已处理，确认继续？',
    '入图确认',
  ).then(() => {
    applying.value = true
    ExtractApi.apply(payload, { warning: false }).then((result: any) => {
      const data = ApiUtil.data(result) ?? {}
      const issues: any[] = data.issues ?? []
      ElNotification({
        title: '入图完成',
        message: `实体 ${data.entityCount ?? 0} 个、关系 ${data.relationshipCount ?? 0} 条${issues.length ? `，${issues.length} 个分组失败` : ''}`,
        type: issues.length ? 'warning' : 'success',
      })
      // 入图后标记数据源为已处理
      if (detail.value) ExtractApi.sourceMark({ id: detail.value.id, processStatus: 1 }, { warning: false }).catch(() => {})
      handleSources()
    }).catch((result: any) => {
      ElNotification({ title: '入图失败', message: ApiUtil.message(result), type: 'error' })
    }).finally(() => {
      applying.value = false
    })
  }).catch(() => {})
}

watch(autoPreview, (value) => {
  if (value) handlePreview()
})

onMounted(() => {
  handleOntologies()
  handleSources()
})
</script>

<template>
  <div class="wb-page">
    <LayoutHeading title="知识抽取" description="维护数据源、即时预览识别结果，人工确认后按主键入图">
      <template #extra>
        <div class="wb-config">
          <div class="wb-config__item">
            <span class="wb-label">本体</span>
            <el-select v-model="ontologyId" filterable placeholder="请选择本体" style="width: 170px" @change="handleModel">
              <el-option v-for="item in ontologies" :key="item.id" :value="item.id" :label="item.name" />
            </el-select>
          </div>
          <div class="wb-config__item">
            <span class="wb-label">抽取方式</span>
            <el-select v-model="mode" style="width: 130px">
              <el-option value="RULE" label="规则 + 词典" />
              <el-option value="LLM" label="大模型（待接入）" />
            </el-select>
          </div>
          <div class="wb-config__item">
            <span class="wb-label">置信度阈值</span>
            <el-slider v-model="threshold" :min="0.5" :max="0.95" :step="0.05" style="width: 96px" />
            <span class="wb-config__value">{{ Number(threshold).toFixed(2) }}</span>
          </div>
          <div class="wb-config__item">
            <el-tooltip content="开启后切换数据源会自动重新抽取" placement="top">
              <el-checkbox v-model="autoPreview">自动预览</el-checkbox>
            </el-tooltip>
          </div>
          <el-button type="primary" :icon="ElementPlusIcons.Search" :loading="extracting" @click="handlePreview">开始抽取</el-button>
          <el-button v-permit="'kg:extract:apply'" type="success" :loading="applying" @click="handleApply">入图</el-button>
        </div>
      </template>
    </LayoutHeading>

    <el-card :bordered="false" shadow="never" class="wb-card">
      <el-splitter class="wb-body">
        <el-splitter-panel size="290px" min="240px" max="420px" collapsible>
          <div class="wb-sources">
            <div class="wb-pane__head">
              <span>数据源</span>
              <span class="wb-pane__count">{{ pagination.total }}</span>
              <el-button
                v-permit="'kg:extract:add'"
                class="wb-pane__head-action"
                link
                :icon="ElementPlusIcons.Plus"
                @click="handleAddSource"
              />
            </div>
            <div class="wb-sources__filter">
              <el-input v-model="listQuery.keyword" placeholder="检索名称" clearable size="small" @keyup.enter="handleSearch">
                <template #prefix><el-icon><ElementPlusIcons.Search /></el-icon></template>
              </el-input>
              <div class="wb-sources__filter-row">
                <el-select v-model="listQuery.processStatus" size="small" placeholder="处理状态" @change="handleSearch">
                  <el-option value="" label="全部状态" />
                  <el-option :value="0" label="未处理" />
                  <el-option :value="1" label="已处理" />
                </el-select>
                <el-select v-model="listQuery.sort" size="small" @change="handleSearch">
                  <el-option value="createdTime" label="按录入时间" />
                  <el-option value="processTime" label="按处理时间" />
                </el-select>
              </div>
            </div>
            <el-scrollbar class="wb-sources__list">
              <div
                v-for="item in sources"
                :key="item.id"
                class="wb-source"
                :class="{ 'wb-source--active': item.id === detail?.id }"
                @click="handleSelectSource(item)"
              >
                <span class="wb-source__main">
                  <span class="wb-source__name">{{ item.name }}</span>
                  <span class="wb-source__meta">
                    {{ item.size ?? 0 }} 字 · {{ item.type === 'FILE' ? '文件' : '文本' }} ·
                    {{ DateUtil.format(item.createdTime, 'MM-dd HH:mm') }}
                  </span>
                </span>
                <el-tag size="small" effect="plain" :type="item.processStatus ? 'success' : 'info'">
                  {{ statusText(item.processStatus) }}
                </el-tag>
              </div>
              <div v-if="!sources.length" class="wb-source__empty">
                <el-empty description="暂无数据源" :image-size="60">
                  <el-button v-permit="'kg:extract:add'" type="primary" size="small" @click="handleAddSource">新增数据源</el-button>
                </el-empty>
              </div>
            </el-scrollbar>
            <div class="wb-sources__page">
              <span class="wb-sources__page-tip">共 {{ pagination.total }} 条</span>
              <el-pagination
                small
                layout="prev, pager, next"
                :total="pagination.total"
                :current-page="pagination.page"
                :page-size="pagination.pageSize"
                @current-change="handlePageChange"
              />
            </div>
          </div>
        </el-splitter-panel>

        <el-splitter-panel>
          <div class="wb-doc">
            <div class="wb-pane__head">
              <span>{{ detail ? detail.name : '未选择数据源' }}</span>
              <el-tag v-if="detail" size="small" effect="plain" :type="detail.processStatus ? 'success' : 'info'">
                {{ statusText(detail.processStatus) }}
              </el-tag>
              <div class="wb-doc__actions">
                <el-button v-if="detail" v-permit="'kg:extract:modify'" size="small" @click="handleEditSource(detail)">编辑</el-button>
                <el-button v-if="detail" v-permit="'kg:extract:delete'" size="small" type="danger" plain @click="handleDeleteSource(detail)">删除</el-button>
                <el-button
                  v-if="detail"
                  v-permit="'kg:extract:run'"
                  size="small"
                  :loading="marking"
                  @click="handleMark(detail.processStatus ? 0 : 1)"
                >{{ detail.processStatus ? '标记未处理' : '标记已处理' }}</el-button>
              </div>
            </div>
            <el-scrollbar class="wb-doc__scroll">
              <div
                v-if="detail"
                class="wb-doc__content"
                v-html="markedHtml"
                @mouseover="handleMarkEnter"
                @mouseleave="handleMarkLeave"
                @click="handleMarkClick"
              ></div>
              <div v-if="detail && (preview.rows ?? []).length" class="wb-doc__legend">
                <span v-for="item in entities" :key="item.label" class="wb-doc__legend-item">
                  <i :style="{ background: entityColor(item.label) }"></i>{{ item.name || item.label }}
                </span>
                <span class="wb-tip">未采纳的标记为灰色，点击可定位右侧候选</span>
              </div>
              <el-empty v-if="!detail" description="在左侧选择数据源查看内容，或点击右上角 + 新增" :image-size="80" />
            </el-scrollbar>
          </div>
        </el-splitter-panel>

        <el-splitter-panel size="420px" min="320px" max="640px" collapsible>
          <el-splitter layout="vertical" class="wb-side">
            <el-splitter-panel>
              <div class="wb-result">
                <div class="wb-pane__head">
                  <span>抽取结果</span>
                  <span class="wb-tip">
                    实体 {{ preview.entityCount ?? 0 }} · 关系 {{ preview.relationshipCount ?? 0 }} ·
                    属性 {{ preview.attributeCount ?? 0 }} · 已采纳 {{ acceptedCount }}
                  </span>
                  <el-button
                    v-if="detail"
                    size="small"
                    text
                    :icon="showDetail ? ElementPlusIcons.ArrowDown : ElementPlusIcons.ArrowUp"
                    @click="showDetail = !showDetail"
                  >{{ showDetail ? '收起详情' : '详细信息' }}</el-button>
                </div>
                <div class="wb-result__tabs">
                  <el-tabs v-model="activeKind">
                    <el-tab-pane v-for="item in KIND_LIST" :key="item.value" :name="item.value">
                      <template #label>{{ item.label }} {{ kindCount(item.value) }}</template>
                    </el-tab-pane>
                  </el-tabs>
                  <el-space>
                    <el-button size="small" text @click="handleToggleAll(true)">全选</el-button>
                    <el-button size="small" text @click="handleToggleAll(false)">全不选</el-button>
                  </el-space>
                </div>
                <el-scrollbar class="wb-result__list">
                  <div
                    v-for="(item, index) in rows"
                    :key="rowKey(item, index)"
                    class="wb-item"
                    :data-key="rowKey(item, index)"
                    :class="{ 'wb-item--active': rowKey(item, index) === markKey }"
                    @mouseenter="markKey = rowKey(item, index)"
                    @mouseleave="markKey = ''"
                  >
                    <div class="wb-item__row">
                      <span class="wb-item__title">{{ itemTitle(item) }}</span>
                      <span class="wb-item__conf">{{ (Number(item.confidence ?? 0) * 100).toFixed(0) }}%</span>
                      <el-tooltip content="采纳后才能入图" placement="top">
                        <el-switch
                          size="small"
                          :model-value="accepted[rowKey(item, index)] ?? false"
                          @update:model-value="(value: any) => accepted[rowKey(item, index)] = value"
                        />
                      </el-tooltip>
                    </div>
                    <div class="wb-item__detail">{{ itemDetail(item) }}</div>
                  </div>
                  <el-empty v-if="!rows.length" :image-size="60"
                    :description="detail ? '暂无结果，点击右上角「开始抽取」' : '请先在左侧选择数据源'">
                    <el-button v-if="detail" type="primary" size="small" :loading="extracting" @click="handlePreview">开始抽取</el-button>
                  </el-empty>
                </el-scrollbar>
              </div>
            </el-splitter-panel>
            <el-splitter-panel v-if="detail && showDetail" size="150px" min="110px" max="60%">
              <div class="wb-detail">
                <div class="wb-detail__title">详细信息</div>
                <el-scrollbar class="wb-detail__body">
                  <el-descriptions :column="2" border size="small" label-width="76px">
                    <el-descriptions-item label="录入人">{{ detail.createdUserInfo?.name || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="录入时间">{{ DateUtil.format(detail.createdTime) }}</el-descriptions-item>
                    <el-descriptions-item label="处理人">{{ detail.processUserInfo?.name || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="处理时间">{{ detail.processTime ? DateUtil.format(detail.processTime) : '-' }}</el-descriptions-item>
                    <el-descriptions-item label="处理状态">{{ statusText(detail.processStatus) }}</el-descriptions-item>
                    <el-descriptions-item label="字符数">{{ detail.size ?? 0 }}</el-descriptions-item>
                  </el-descriptions>
                  <div v-if="detail.filename" class="wb-detail__file">文件：{{ detail.filename }}</div>
                </el-scrollbar>
              </div>
            </el-splitter-panel>
          </el-splitter>
        </el-splitter-panel>
      </el-splitter>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" top="6vh" :close-on-click-modal="false">
      <el-form :model="sourceForm" label-position="top">
        <el-form-item required>
          <template #label><span>名称</span><LayoutHelp text="数据源名称，用于在工作台列表里区分输入文本" /></template>
          <el-input v-model="sourceForm.name" placeholder="例如：星辰科技企业公告" />
        </el-form-item>
        <el-form-item>
          <template #label><span>上传文件</span><LayoutHelp text="支持 txt / csv 直接读取，xlsx 按行列拼接为文本（制表符分隔）" /></template>
          <el-upload :auto-upload="false" :show-file-list="false" accept=".txt,.csv,.xlsx" :on-change="handleUpload">
            <el-button :icon="ElementPlusIcons.Upload">选择文件自动填充</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item required>
          <template #label><span>文本内容</span><LayoutHelp text="抽取以这里的内容为准，单次不超过 20 万字符" /></template>
          <el-input v-model="sourceForm.content" type="textarea" :rows="14" spellcheck="false" placeholder="粘贴或上传待抽取的文本" />
        </el-form-item>
        <div class="wb-tip">共 {{ (sourceForm.content ?? '').length }} 个字符</div>
      </el-form>
      <template #footer>
        <el-space>
          <el-button type="primary" :loading="saving" @click="handleSubmitSource">确定</el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.wb-label { font-size: 13px; color: var(--el-text-color-secondary); }
.wb-tip { font-size: 12px; color: var(--el-text-color-secondary); }
/* 顶部配置 */
.wb-config {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 20px;
  &__item {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  &__value {
    width: 34px;
    font-size: 12px;
    color: var(--el-text-color-regular);
  }
}
.wb-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - var(--fs-layout-header-height) - 40px);
  min-height: 520px;
  :deep(.fs-heading) { margin-bottom: 0; }
}
.wb-card {
  flex: 1;
  min-height: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  :deep(.el-card__body) {
    height: 100%;
    padding: 0;
  }
}
.wb-body { height: 100%; }
.wb-pane__head {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 14px;
  box-sizing: border-box;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  > .wb-tip { font-weight: 400; margin-left: auto; }
  &-action { margin-left: auto; }
}
.wb-pane__count {
  padding: 0 6px;
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border-radius: 8px;
}
/* 数据源 */
.wb-sources {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-right: 1px solid var(--el-border-color-lighter);
  &__filter {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 10px 12px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    background: var(--el-fill-color-lighter);
  }
  &__filter-row {
    display: flex;
    gap: 6px;
    > .el-select { flex: 1; }
  }
  &__list { flex: 1; min-height: 0; }
  &__page {
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 8px 10px;
    border-top: 1px solid var(--el-border-color-lighter);
  }
  &__page-tip { font-size: 12px; color: var(--el-text-color-secondary); }
}
.wb-source {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 4px 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color .2s;
  &:hover { background: var(--el-fill-color-light); }
  &--active { background: var(--el-color-primary-light-9); }
  &__main { display: flex; flex-direction: column; min-width: 0; }
  &__name {
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &__meta { margin-top: 2px; font-size: 11px; color: var(--el-text-color-secondary); }
  &__empty { padding: 12px; font-size: 12px; color: var(--el-text-color-placeholder); }
}
/* 原文与详情 */
.wb-doc {
  display: flex;
  flex-direction: column;
  height: 100%;
  &__actions { margin-left: auto; }
  &__scroll { flex: 1; min-height: 0; padding: 14px 16px; }
  &__content {
    padding: 14px 16px;
    font-size: 13.5px;
    line-height: 2;
    color: var(--el-text-color-primary);
    white-space: pre-wrap;
    word-break: break-all;
    background: var(--el-fill-color-blank);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    :deep(.wb-mark) {
      padding: 0 2px;
      color: var(--mark-color);
      background: color-mix(in srgb, var(--mark-color) 14%, transparent);
      border-bottom: 2px solid var(--mark-color);
      border-radius: 3px;
      cursor: pointer;
      transition: background-color .2s;
    }
    :deep(.wb-mark--ignored) {
      color: var(--el-text-color-placeholder);
      background: transparent;
      border-bottom: 1px dashed var(--el-text-color-placeholder);
      text-decoration: line-through;
    }
    :deep(.wb-mark--active) {
      background: color-mix(in srgb, var(--mark-color) 32%, transparent);
    }
  }
  &__legend {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 14px;
    margin-top: 12px;
    padding: 8px 12px;
    background: var(--el-fill-color-lighter);
    border-radius: 6px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
  &__legend-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    i { display: inline-block; width: 8px; height: 8px; border-radius: 50%; }
  }
}
.wb-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  &__title {
    flex: none;
    display: flex;
    align-items: center;
    height: 36px;
    padding: 0 14px;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    background: var(--el-fill-color-lighter);
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  &__body { flex: 1; min-height: 0; padding: 12px 14px; }
  &__file { margin-top: 8px; font-size: 12px; color: var(--el-text-color-secondary); word-break: break-all; }
}
/* 结果 */
.wb-side { height: 100%; }
.wb-result {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-left: 1px solid var(--el-border-color-lighter);
  &__tabs {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    height: 44px;
    padding: 0 14px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    :deep(.el-tabs__header) { margin-bottom: 0; }
    :deep(.el-tabs__nav-wrap::after) { display: none; }
  }
  &__list { flex: 1; min-height: 0; padding: 10px 12px 14px; }
}
.wb-item {
  padding: 10px 12px;
  margin-bottom: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  transition: border-color .2s, background-color .2s;
  &:hover { border-color: var(--el-color-primary-light-5); }
  &--active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
  &__row { display: flex; align-items: center; gap: 8px; }
  &__title {
    min-width: 0;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &__conf { margin-left: auto; font-size: 12px; color: var(--el-text-color-secondary); }
  &__detail {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.7;
    color: var(--el-text-color-secondary);
    word-break: break-all;
  }
}
@media (max-width: 1280px) {
  .wb-page { height: auto; min-height: 0; }
  .wb-card {
    flex: none;
    :deep(.el-card__body) { height: auto; }
  }
  .wb-body { height: 560px; }
}
</style>
