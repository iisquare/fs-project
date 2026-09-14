<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import ApiUtil from '@/utils/ApiUtil'
import WorkflowApi from '@/api/oa/WorkflowApi'
import UserApi from '@/api/member/UserApi'
import RoleApi from '@/api/member/RoleApi'

const model: any = defineModel()
const props = defineProps<{
  bpmn: any,
  element: any,
  workflow: any,
}>()

const activeKey = ref('user')
let idGenerator = 0
let volatile = 0
let syncing = false
const tags = ref<any[]>([])
const loading = ref(true)
const visible = ref(false)
const keyword = ref('')
const tableRef = ref<any>()
const rows = ref<any[]>([])
const icons: any = { user: 'User', role: 'Avatar', variable: 'DataLine' }
const variables: any = { ['$' + '{submitter}']: { id: '$' + '{submitter}', name: '发起人' } }
const placeholders: any = { user: '用户名称', role: '角色名称', variable: '变量名称' }

const currentTags = computed(() => {
  return tags.value.filter(tag => tag.type === activeKey.value)
})

const parseTags = () => {
  const list: any[] = []
  const args: any = { userIds: [], roleIds: [] }
  const regexp = /^\$\{\w+\}$/
  String(model.value || '').split(',').forEach(tag => {
    if (!tag) return
    if (tag.match(regexp)) {
      list.push({
        id: idGenerator++, type: 'variable', icon: icons.variable, value: tag,
        label: '$-' + (variables[tag]?.name ?? '未知变量')
      })
      return
    }
    const strs = tag.split('-')
    if (strs.length !== 2) return
    const id = Number.parseInt(strs[1])
    if (!Number.isInteger(id)) return
    switch (strs[0]) {
      case 'user':
        args.userIds.push(id)
        list.push({ id: idGenerator++, type: 'user', icon: icons.user, value: id, label: id + '-未知用户' })
        break
      case 'role':
        args.roleIds.push(id)
        list.push({ id: idGenerator++, type: 'role', icon: icons.role, value: id, label: id + '-未知角色' })
        break
    }
  })
  return { list, args }
}

watch(() => props.element, () => {
  loading.value = true
  const current = ++volatile
  const { list, args } = parseTags()
  WorkflowApi.candidateInfos(args).then((result: any) => {
    if (current !== volatile) return false
    const data = ApiUtil.data(result) || {}
    list.forEach((item: any) => {
      const value = item.value + ''
      if (item.type === 'user' && data.users && data.users[value]) {
        item.label = value + '-' + data.users[value].name
      }
      if (item.type === 'role' && data.roles && data.roles[value]) {
        item.label = value + '-' + data.roles[value].name
      }
    })
    tags.value = list
    loading.value = false
    return true
  }).catch(() => {
    if (current !== volatile) return false
    tags.value = list
    loading.value = false
    return false
  })
}, { immediate: true })

watch(tags, (list) => {
  const result: string[] = []
  list.forEach((tag: any) => {
    switch (tag.type) {
      case 'user':
      case 'role':
        result.push(`${tag.type}-${tag.value}`)
        break
      case 'variable':
        result.push(tag.value)
        break
    }
  })
  model.value = result.join(',')
}, { deep: true })

const syncSelection = () => {
  syncing = true
  tableRef.value?.clearSelection()
  rows.value.forEach((row: any) => {
    const hit = tags.value.some(tag => tag.type === activeKey.value && tag.value === row.id)
    if (hit) tableRef.value?.toggleRowSelection(row, true)
  })
  nextTick(() => { syncing = false })
}

const onSelectionChange = (selection: any[]) => {
  if (syncing) return false
  const allMap: any = {}
  rows.value.forEach((row: any) => { allMap[row.id] = row })
  const selectedMap: any = {}
  selection.forEach((row: any) => { selectedMap[row.id] = row })
  for (let index = tags.value.length - 1; index >= 0; index--) {
    const tag = tags.value[index]
    if (tag.type !== activeKey.value) continue
    if (selectedMap[tag.value]) { // 已选中
      delete selectedMap[tag.value]
      continue
    }
    if (!allMap[tag.value]) continue // 标签不在当前列表中
    tags.value.splice(index, 1) // 移除列表中未选中的标签
  }
  for (const key in selectedMap) { // 加入标签中不存在的选中元素
    const row = selectedMap[key]
    const label = (activeKey.value === 'variable' ? '$' : row.id) + '-' + row.name
    tags.value.push({ id: idGenerator++, type: activeKey.value, icon: icons[activeKey.value], value: row.id, label })
  }
  return true
}

const search = (value: string) => {
  loading.value = true
  const services: any = {
    user: UserApi,
    role: RoleApi,
    variable: {
      list (param: any) {
        const list = Object.values(variables).filter((item: any) => item.name.indexOf(param.name) !== -1)
        return Promise.resolve(ApiUtil.result(0, null, { rows: list, total: list.length }))
      }
    }
  }
  services[activeKey.value].list({ name: value, pageSize: 6 }).then((result: any) => {
    if (ApiUtil.succeed(result)) {
      rows.value = ApiUtil.data(result).rows
    }
    loading.value = false
    syncSelection()
  }).catch(() => {
    loading.value = false
  })
}

const handleClick = () => {
  if (loading.value) return false
  visible.value = true
  keyword.value = ''
  search('')
  return true
}

const removeTag = (tag: any) => {
  for (let index = tags.value.length - 1; index >= 0; index--) {
    if (tags.value[index].id !== tag.id) continue
    tags.value.splice(index, 1)
    return true
  }
  return false
}

watch(activeKey, () => {
  if (!visible.value) return false
  keyword.value = ''
  search('')
  return true
})

watch(visible, (val) => {
  if (!val) keyword.value = ''
})
</script>

<template>
  <section>
    <div class="fs-candidate-tags" @click="handleClick">
      <div class="fs-candidate-empty" v-if="loading">
        <el-button link type="primary" :loading="true">正在载入数据...</el-button>
      </div>
      <template v-else-if="tags.length === 0">
        <div class="fs-candidate-empty">
          <el-button link type="primary" :icon="ElementPlusIcons.Plus">点击设置负责人</el-button>
        </div>
      </template>
      <template v-else>
        <el-tag class="fs-candidate-tag" v-for="tag in tags" :key="tag.id">
          <LayoutIcon :name="tag.icon" :class="'fs-tag-icon fs-icon-' + tag.type" />{{ tag.label }}
        </el-tag>
      </template>
    </div>
    <el-dialog title="候选人信息配置" v-model="visible" :width="800" :footer="null" :close-on-click-modal="false">
      <div class="fs-candidate-tags fs-candidate-editor">
        <el-tag class="fs-candidate-tag" v-for="tag in tags" :key="tag.id" closable @close="() => removeTag(tag)">
          <LayoutIcon :name="tag.icon" :class="'fs-tag-icon fs-icon-' + tag.type" />{{ tag.label }}
        </el-tag>
      </div>
      <div class="fs-candidate-card">
        <el-tabs v-model="activeKey">
          <el-tab-pane label="用户" name="user" />
          <el-tab-pane label="角色" name="role" />
          <el-tab-pane label="变量" name="variable" />
        </el-tabs>
        <div class="fs-card-layout">
          <div class="fs-card-left">
            <div class="fs-card-title">已选{{ activeKey === 'user' ? '用户' : (activeKey === 'role' ? '角色' : '变量') }}</div>
            <div class="fs-card-content">
              <ul>
                <li v-for="tag in currentTags" :key="tag.id">
                  <el-tag class="fs-candidate-tag" closable @close="() => removeTag(tag)">
                    <LayoutIcon :name="tag.icon" :class="'fs-tag-icon fs-icon-' + tag.type" />{{ tag.label }}
                  </el-tag>
                </li>
              </ul>
            </div>
          </div>
          <div class="fs-card-right">
            <div class="fs-card-title">
              <el-input v-model="keyword" :placeholder="placeholders[activeKey]" class="fs-candidate-search" clearable @keyup.enter="() => search(keyword)">
                <template #append>
                  <el-button :icon="ElementPlusIcons.Search" @click="() => search(keyword)" />
                </template>
              </el-input>
            </div>
            <div class="fs-card-content" v-loading="loading">
              <el-table
                ref="tableRef"
                :data="rows"
                row-key="id"
                :border="true"
                @selection-change="onSelectionChange">
                <el-table-column type="selection" width="45" />
                <el-table-column prop="id" label="ID" />
                <el-table-column prop="name" label="名称" />
              </el-table>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<style lang="scss" scoped>
.fs-candidate-tags {
  cursor: pointer;
  min-height: 100px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 10px;
}
// 无标签时的提示按钮居中显示
.fs-candidate-empty {
  min-height: 80px;
  @include flex-center();
}
// 已选标签：加大高度并留出图标空间，避免内容拥挤
.fs-candidate-tag {
  height: 28px;
  margin: 0px 8px 8px 0px;
  padding: 0px 10px 0px 4px;
  border-radius: 14px;
  font-size: 12px;
  // 图标与文字同处一行时按基线对齐会偏上/偏下，改为 flex 居中
  :deep(.el-tag__content) {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
}
.fs-tag-icon {
  flex: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  vertical-align: middle;
}
.fs-icon-user {
  color: white;
  background-color: #0db3a6;
}
.fs-icon-role {
  color: white;
  background-color: #5d9cee;
}
.fs-icon-variable {
  color: white;
  background-color: #fa0;
}
.fs-candidate-editor {
  height: 80px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.fs-candidate-card {
  margin-top: 10px;
  border: 1px solid #e8e8e8;
  :deep(.el-tabs__header) {
    margin-bottom: 0px;
  }
  // 页签仅作为切换入口，内容统一在下方渲染，隐藏空的内容区避免大片留白
  :deep(.el-tabs__content) {
    display: none;
  }
  :deep(.el-tabs__item) {
    padding: 0px 15px;
  }
  .fs-card-layout {
    width: 100%;
    height: 380px;
    display: flex;
    .fs-card-left {
      width: 250px;
      height: 100%;
      overflow: auto;
      border-right: 1px solid #e8e8e8;
      ul {
        position: relative;
        padding: 0;
        margin: 0;
        list-style: none;
      }
      li {
        padding: 5px;
      }
    }
    .fs-card-right {
      width: calc(100% - 250px);
      height: 100%;
    }
    .fs-card-title {
      height: 45px;
      line-height: 45px;
      padding: 0px 10px;
      border-bottom: 1px solid #e8e8e8;
    }
    .fs-card-content {
      overflow: auto;
      height: calc(100% - 45px);
    }
    .fs-candidate-search {
      width: 240px;
    }
  }
}
</style>
