<script setup lang="ts">
/**
 * 本体设计器左侧大纲
 *
 * 以实体、关系列表呈现当前画布结构：点击定位并选中，支持新增实体、新增关系与删除，
 * 取代原先只能拖拽"实体节点"的组件库面板。
 */
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'

const props = defineProps<{
  instance?: any,
  activeItem?: any,
}>()
const emit = defineEmits(['update:activeItem'])

const graph = ref<any>(null)
const entities = ref<any[]>([])
const relationships = ref<any[]>([])
// model:reseted 在载入已有本体（graph.fromJSON）时触发，必须监听，否则大纲不会刷新
const EVENTS = ['node:added', 'node:removed', 'edge:added', 'edge:removed',
  'node:change:data', 'edge:change:data', 'cell:removed', 'model:reseted', 'model:updated']

const dataOf = (cell: any) => (cell?.getData ? cell.getData() : {}) ?? {}
const cellId = (edge: any, key: 'Source' | 'Target') => {
  const value = typeof edge[`get${key}CellId`] === 'function'
    ? edge[`get${key}CellId`]() : edge[`get${key}Cell`]()?.id
  return value ?? ''
}
const captionOf = (id: string) => {
  const data = dataOf(graph.value?.getCellById(id))
  return data.name || data.label || '未命名'
}

const refresh = () => {
  const instance = graph.value
  if (!instance) {
    entities.value = []
    relationships.value = []
    return
  }
  entities.value = instance.getNodes().filter((node: any) => 'kg-node' === node.shape).map((node: any) => {
    const data = dataOf(node)
    return {
      id: node.id,
      name: data.name || '',
      label: data.label || '',
      fieldCount: (data.fields ?? []).length,
    }
  })
  relationships.value = instance.getEdges()
    .filter((edge: any) => ['flow-edge', 'edge'].includes(edge.shape))
    .map((edge: any) => {
      const data = dataOf(edge)
      return {
        id: edge.id,
        name: data.name || '',
        label: data.label || '',
        fieldCount: (data.fields ?? []).length,
        source: captionOf(cellId(edge, 'Source')),
        target: captionOf(cellId(edge, 'Target')),
      }
    })
}

watch(() => props.instance, (value: any) => {
  const next = value?.flow?.graph ?? null
  if (next === graph.value) return
  if (graph.value) EVENTS.forEach(event => graph.value.off(event, refresh))
  graph.value = next
  if (!next) return refresh()
  EVENTS.forEach(event => next.on(event, refresh))
  refresh()
}, { immediate: true })

onUnmounted(() => {
  if (!graph.value) return
  EVENTS.forEach(event => graph.value.off(event, refresh))
  graph.value = null
})

const activeId = computed(() => props.activeItem?.id ?? '')

const keyword = ref('')
const matched = (item: any) => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return true
  return [item.name, item.label, item.source, item.target]
    .filter((value: any) => !!value)
    .some((value: any) => String(value).toLowerCase().includes(text))
}
const filteredEntities = computed(() => entities.value.filter(matched))
const filteredRelationships = computed(() => relationships.value.filter(matched))
const searching = computed(() => keyword.value.trim().length > 0)
const countText = (list: any[], total: number) => searching.value ? `${list.length}/${total}` : `${total}`

const handleSelect = (item: any) => {
  const cell: any = graph.value?.getCellById(item.id)
  if (!cell) {
    refresh()
    return
  }
  emit('update:activeItem', props.instance.flow.cell2meta(cell))
  props.instance.flow.select(cell)
  if (typeof graph.value.centerCell === 'function') graph.value.centerCell(cell)
}

const handleDelete = (item: any) => {
  props.instance.flow.remove({ id: item.id })
  refresh()
}

/**
 * 新元素落在当前视口中央，与已有元素重叠时向下错开
 */
const nextPoint = (width: number, height: number) => {
  const instance = graph.value
  const container = instance.container
  const centerX = (container?.clientWidth ?? 800) / 2
  const centerY = (container?.clientHeight ?? 600) / 2
  const point = typeof instance.clientToLocal === 'function'
    ? instance.clientToLocal(centerX, centerY) : instance.pageToLocal(centerX, centerY)
  let x = point.x - width / 2
  let y = point.y - height / 2
  const boxes = instance.getNodes().map((node: any) => node.getBBox())
  const overlap = () => boxes.some((box: any) => x < box.x + box.width + 20 && x + width + 20 > box.x
    && y < box.y + box.height + 20 && y + height + 20 > box.y)
  let guard = 0
  while (overlap() && guard++ < 50) {
    x += 40
    y += 160
  }
  return { x, y }
}

const handleAddEntity = () => {
  const rect = nextPoint(260, 88)
  const node: any = graph.value.createNode(Object.assign({
    shape: 'kg-node',
    width: 260,
    height: 88,
    zIndex: (props.instance.flow.counter += 1),
    data: {
      type: 'Node',
      name: `实体${entities.value.length + 1}`,
      label: '',
      description: '',
      icon: '',
      color: '',
      primaryField: '',
      captionField: '',
      extendable: false,
      extendableLabels: false,
      fields: [],
    },
  }, rect))
  graph.value.addNode(node)
  handleSelect({ id: node.id })
  ElMessage.success('已新增实体，请在右侧完善名称与标签')
}

const relationVisible = ref(false)
const relationForm = reactive({ source: '', target: '', name: '', label: '', description: '' })

const handleOpenRelation = () => {
  if (entities.value.length < 2) return ElMessage.warning('请先创建至少两个实体，再建立关系')
  Object.assign(relationForm, {
    source: entities.value[0].id,
    target: entities.value[1].id,
    name: '',
    label: '',
    description: '',
  })
  relationVisible.value = true
}

const handleAddRelationship = () => {
  if (!relationForm.label) return ElMessage.warning('请填写关系标签')
  if (relationForm.source === relationForm.target) return ElMessage.warning('起点与终点不能是同一个实体')
  if (relationships.value.some((item: any) => item.label === relationForm.label)) {
    return ElMessage.warning(`关系标签[${relationForm.label}]已存在`)
  }
  const edge: any = graph.value.createEdge({
    shape: 'flow-edge',
    source: { cell: relationForm.source, port: 'right' },
    target: { cell: relationForm.target, port: 'left' },
    data: {
      name: relationForm.name || relationForm.label,
      label: relationForm.label,
      description: relationForm.description,
      mergeFields: [],
      cascadeDelete: false,
      fields: [],
    },
  })
  graph.value.addEdge(edge)
  relationVisible.value = false
  handleSelect({ id: edge.id })
  ElMessage.success('已新增关系，可在右侧补充属性')
}
</script>

<template>
  <div class="outline">
    <div class="outline-search">
      <el-input
        v-model="keyword"
        :prefix-icon="ElementPlusIcons.Search"
        placeholder="搜索实体或关系"
        clearable
      />
    </div>
    <div class="outline-group">
      <div class="outline-head">
        <span>实体（{{ countText(filteredEntities, entities.length) }}）</span>
        <el-tooltip content="在画布中新增一个实体节点" placement="top">
          <el-button link :icon="ElementPlusIcons.Plus" @click="handleAddEntity" />
        </el-tooltip>
      </div>
      <div v-if="!filteredEntities.length" class="outline-empty">
        {{ searching ? '没有匹配的实体' : '暂无实体，点击右上角 + 新增' }}
      </div>
      <div
        v-for="item in filteredEntities"
        :key="item.id"
        class="outline-item"
        :class="{ 'outline-item--active': activeId === item.id }"
        @click="handleSelect(item)"
      >
        <span class="outline-item__main">
          <span class="outline-item__title">{{ item.name || item.label || '未命名实体' }}</span>
          <span class="outline-item__info">
            {{ item.label || '未设置标签' }}
            <em>· {{ item.fieldCount }} 个字段</em>
          </span>
        </span>
        <el-popconfirm title="删除该实体及其关系？" width="180" @confirm="handleDelete(item)">
          <template #reference>
            <el-button link class="outline-item__action" :icon="ElementPlusIcons.Delete" @click.stop />
          </template>
        </el-popconfirm>
      </div>
    </div>

    <div class="outline-group">
      <div class="outline-head">
        <span>关系（{{ countText(filteredRelationships, relationships.length) }}）</span>
        <el-tooltip content="选择两端实体后新增关系" placement="top">
          <el-button link :icon="ElementPlusIcons.Plus" @click="handleOpenRelation" />
        </el-tooltip>
      </div>
      <div v-if="!filteredRelationships.length" class="outline-empty">
        {{ searching ? '没有匹配的关系' : '暂无关系，点击右上角 + 新增' }}
      </div>
      <div
        v-for="item in filteredRelationships"
        :key="item.id"
        class="outline-item"
        :class="{ 'outline-item--active': activeId === item.id }"
        @click="handleSelect(item)"
      >
        <span class="outline-item__main">
          <span class="outline-item__title">{{ item.name || item.label || '未命名关系' }}</span>
          <span class="outline-item__info">
            {{ item.label || '未设置标签' }}
            <em>· {{ item.fieldCount }} 个字段</em>
          </span>
          <span class="outline-item__info">{{ item.source }} → {{ item.target }}</span>
        </span>
        <el-popconfirm title="删除该关系？" width="180" @confirm="handleDelete(item)">
          <template #reference>
            <el-button link class="outline-item__action" :icon="ElementPlusIcons.Delete" @click.stop />
          </template>
        </el-popconfirm>
      </div>
    </div>
  </div>

  <el-dialog v-model="relationVisible" title="新增关系" width="460px">
    <el-form :model="relationForm" label-position="top">
      <el-form-item label="起点实体" required>
        <el-select v-model="relationForm.source" filterable style="width: 100%">
          <el-option v-for="item in entities" :key="item.id" :value="item.id" :label="item.name || item.label || '未命名实体'" />
        </el-select>
      </el-form-item>
      <el-form-item label="终点实体" required>
        <el-select v-model="relationForm.target" filterable style="width: 100%">
          <el-option v-for="item in entities" :key="item.id" :value="item.id" :label="item.name || item.label || '未命名实体'" />
        </el-select>
      </el-form-item>
      <el-form-item label="关系标签" required>
        <el-input v-model="relationForm.label" placeholder="必填，对应图数据库中的关系类型" />
      </el-form-item>
      <el-form-item label="关系名称">
        <el-input v-model="relationForm.name" placeholder="选填，默认为关系标签" />
      </el-form-item>
      <el-form-item label="关系描述">
        <el-input v-model="relationForm.description" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-space>
        <el-button @click="relationVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddRelationship">确定</el-button>
      </el-space>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.outline {
  padding: 0 0 12px;
  .outline-search {
    position: sticky;
    top: 0;
    z-index: 1;
    padding: 8px 10px;
    background: var(--fs-layout-background-color, #fff);
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
  .outline-group {
    margin-top: 6px;
    & + .outline-group {
      border-top: 1px solid var(--el-border-color-lighter);
    }
  }
  .outline-head {
    height: 34px;
    padding: 0 12px;
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
  .outline-empty {
    padding: 6px 12px 10px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
  .outline-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
    margin: 0 6px;
    padding: 6px 6px 6px 8px;
    border-radius: 6px;
    cursor: pointer;
    &:hover {
      background: var(--el-fill-color-light);
    }
    &--active {
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
    &__main {
      display: flex;
      flex-direction: column;
      min-width: 0;
      line-height: 16px;
    }
    &__title {
      font-size: 13px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    &__info {
      font-size: 11px;
      color: var(--el-text-color-secondary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      > em {
        font-style: normal;
      }
    }
    &__action {
      flex: none;
      color: var(--el-text-color-placeholder);
      &:hover {
        color: var(--el-color-danger);
      }
    }
  }
}
</style>
