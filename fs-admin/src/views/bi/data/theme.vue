<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import DataThemeApi from '@/api/bi/DataThemeApi';
import DatasetApi from '@/api/bi/DatasetApi';
import RoleApi from '@/api/member/RoleApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import UIUtil from '@/utils/UIUtil';
import DataSchemaTable from '@/components/Data/DataSchemaTable.vue';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '主题名称' },
  { prop: 'labels', label: '标签', slot: 'labels' },
  { prop: 'role', label: '授权角色', slot: 'role' },
  { prop: 'datasets', label: '数据集', slot: 'datasets', minWidth: 180 },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render, hide: true },
])
const config: any = ref({
  ready: false,
  sorts: {},
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DataThemeApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  DataThemeApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const emptyContent = () => ({ datasetIds: [], relations: [] })
const relationRowsFromContent = (relations: any[]) => {
  return (relations || []).map((relation: any) => ({
    id: relation.id,
    sourceDatasetId: Number(relation.sourceDatasetId),
    targetDatasetId: Number(relation.targetDatasetId),
    sourceFields: Array.isArray(relation.sourceFields) ? [...relation.sourceFields] : [],
    targetFields: Array.isArray(relation.targetFields) ? [...relation.targetFields] : [],
    description: relation.description || '',
  }))
}

const datasetCache: any = ref({})
const datasetById = (datasetId: any) => datasetCache.value[Number(datasetId)]
const cacheDatasets = (datasets: any[]) => {
  ;(datasets || []).forEach((dataset: any) => {
    if (!dataset) return
    let fields = dataset.fields
    if (typeof fields === 'string') {
      try {
        fields = JSON.parse(fields)
      } catch {
        fields = []
      }
    }
    let pks = dataset.pks
    if (typeof pks === 'string') {
      pks = pks ? pks.split(',').map((item: any) => item.trim()).filter((item: any) => item) : []
    }
    datasetCache.value[Number(dataset.id)] = Object.assign({}, dataset, {
      fields: Array.isArray(fields) ? fields : [],
      pks: Array.isArray(pks) ? pks : [],
      typeText: dataset.typeText || (dataset.type === 'direct' ? '直连' : dataset.type === 'cron' ? '定时同步' : dataset.type || '未知'),
    })
  })
}
const selectedDatasets = computed(() => {
  return (form.value.content?.datasetIds || []).map((id: any) => {
    return datasetById(id) || { id, name: String(id) }
  })
})
const fieldsOfDataset = (datasetId: any) => datasetById(datasetId)?.fields || []
const selectedName = (datasetId: any) => datasetById(datasetId)?.name || datasetId

const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入主题名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})
const pendingDatasetId: any = ref(null)
const pendingDataset: any = ref(null)
const relationRows = ref<any[]>([])

const loadInfo = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: String(scope.row.status),
    labels: scope.row.labels || [],
    roleIds: scope.row.roleIds || [],
    content: emptyContent(),
  })
  relationRows.value = []
  pendingDatasetId.value = null
  pendingDataset.value = null
  cacheDatasets(scope.row.datasets)
  return DataThemeApi.info(scope.row.id).then((result: any) => {
    const info = ApiUtil.data(result)
      Object.assign(form.value, {
      id: info.id,
      name: info.name,
      content: info.content || emptyContent(),
      sort: info.sort,
      status: String(info.status),
      description: info.description,
    })
    cacheDatasets(info.datasets)
    relationRows.value = relationRowsFromContent(form.value.content.relations)
  })
}
const handleAdd = () => {
  form.value = {
    status: '1',
    sort: 0,
    labels: [],
    roleIds: [],
    content: emptyContent(),
  }
  relationRows.value = []
  pendingDatasetId.value = null
  pendingDataset.value = null
  formVisible.value = true
}
const handleShow = (scope: any) => {
  loadInfo(scope).catch(() => {})
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  loadInfo(scope).catch(() => {})
  formVisible.value = true
}
const handleAddDataset = () => {
  const dataset = pendingDataset.value
  if (!dataset) {
    ElMessage.warning('请先检索并选择一个数据集')
    return
  }
  const id = Number(dataset.id)
  if (form.value.content.datasetIds.includes(id)) {
    ElMessage.warning('该数据集已添加')
    return
  }
  cacheDatasets([dataset])
  form.value.content.datasetIds.push(id)
  pendingDatasetId.value = null
  pendingDataset.value = null
}
const handleRemoveDataset = (datasetId: any) => {
  const id = Number(datasetId)
  const index = form.value.content.datasetIds.indexOf(id)
  if (index > -1) form.value.content.datasetIds.splice(index, 1)
  relationRows.value = relationRows.value.filter((relation: any) =>
    Number(relation.sourceDatasetId) !== id && Number(relation.targetDatasetId) !== id)
}
const handleAddRelation = () => {
  if (form.value.content.datasetIds.length < 2) {
    ElMessage.warning('请先添加两个及以上数据集')
    return
  }
  const source = form.value.content.datasetIds[0]
  const target = form.value.content.datasetIds.find((id: number) => id !== source)
  relationRows.value.push({
    id: UIUtil.uuid('r'),
    sourceDatasetId: source,
    targetDatasetId: target,
    sourceFields: [],
    targetFields: [],
    description: '',
  })
}
const handleRemoveRelation = (index: number) => {
  relationRows.value.splice(index, 1)
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    if (!form.value.content.datasetIds.length) {
      ElMessage.warning('请至少添加一个数据集')
      return
    }
    for (const relation of relationRows.value) {
      if (!relation.sourceDatasetId || !(relation.sourceFields || []).length
        || !relation.targetDatasetId || !(relation.targetFields || []).length) {
        ElMessage.warning('字段关联需要完整的源数据集、源字段、目标数据集、目标字段')
        return
      }
      if ((relation.sourceFields || []).length !== (relation.targetFields || []).length) {
        ElMessage.warning('字段关联两侧字段数量必须一致')
        return
      }
      if (Number(relation.sourceDatasetId) === Number(relation.targetDatasetId)) {
        ElMessage.warning('源数据集与目标数据集不能相同')
        return
      }
    }
    formLoading.value = true
    const content = {
      datasetIds: form.value.content.datasetIds.map(Number),
      relations: relationRows.value.map((relation: any) => ({
        id: relation.id,
        sourceDatasetId: Number(relation.sourceDatasetId),
        targetDatasetId: Number(relation.targetDatasetId),
        sourceFields: relation.sourceFields,
        targetFields: relation.targetFields,
        description: relation.description || '',
      })),
    }
    DataThemeApi.save({
      id: form.value.id,
      name: form.value.name,
      labels: form.value.labels || [],
      roleIds: form.value.roleIds || [],
      sort: form.value.sort,
      status: form.value.status,
      description: form.value.description,
      content: JSON.stringify(content),
    }, { success: true }).then(() => {
      formVisible.value = false
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DataThemeApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'bi:dataTheme:add'" @click="handleAdd" />
        <button-delete v-permit="'bi:dataTheme:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" @change="handleRefresh(true, true)" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #labels="scope">
          <el-space wrap>
            <el-tag v-for="item in (scope.row.labels || [])" :key="item" type="info">{{ item }}</el-tag>
          </el-space>
        </template>
        <template #role="scope">
          <el-space wrap>
            <el-tag v-for="item in (scope.row.roles || [])" :key="item.id">{{ item.name }}</el-tag>
          </el-space>
        </template>
        <template #datasets="scope">
          <el-space wrap>
            <el-tag v-if="scope.row.datasetCount" type="warning">{{ scope.row.datasetCount }} 个</el-tag>
            <span v-else>暂无</span>
          </el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'bi:dataTheme:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'bi:dataTheme:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-form :model="form" label-width="auto">
      <layout-heading title="基础信息" />
      <el-descriptions :column="2" label-width="100px" border>
        <el-descriptions-item label="名称">{{ form.name }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
        <el-descriptions-item label="数据集数量">{{ form.content?.datasetIds?.length }}</el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-space wrap>
            <el-tag v-for="item in (form.labels || [])" :key="item" type="info">{{ item }}</el-tag>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="授权角色">
          <el-space wrap>
            <el-tag v-for="item in (form.roles || [])" :key="item.id">{{ item.name }}</el-tag>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
      </el-descriptions>
      <layout-heading title="数据集" />
      <el-table :data="selectedDatasets" :row-key="(record: any) => record.id" :border="true" table-layout="auto" class="mb-15">
        <el-table-column type="expand">
          <template #default="scope">
            <DataSchemaTable v-if="(scope.row.fields || []).length" :model-value="scope.row.fields" class="dataset-fields" />
            <el-empty v-else :image-size="40" description="暂无字段信息" />
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" />
        <el-table-column prop="name" label="数据集" />
        <el-table-column label="服务方式">
          <template #default="scope">{{ scope.row.typeText || '未知' }}</template>
        </el-table-column>
        <el-table-column label="主键">
          <template #default="scope">
            <span v-if="!(scope.row.pks || []).length">暂无</span>
            <el-space v-else wrap>
              <el-tag v-for="item in scope.row.pks" :key="item" type="info">{{ item }}</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="字段数量">
          <template #default="scope">{{ (scope.row.fields || []).length }}</template>
        </el-table-column>
      </el-table>
      <layout-heading title="字段关联" />
      <el-empty v-if="!relationRows.length" description="暂无字段关联" />
      <el-table v-else :data="relationRows" :border="true" table-layout="auto">
        <el-table-column label="源数据集" min-width="140">
          <template #default="scope">{{ selectedName(scope.row.sourceDatasetId) }}</template>
        </el-table-column>
        <el-table-column label="源数据字段" min-width="140">
          <template #default="scope">
            <el-space wrap>
              <el-tag v-for="field in (scope.row.sourceFields || [])" :key="field" type="info">{{ field }}</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="目标数据集" min-width="140">
          <template #default="scope">{{ selectedName(scope.row.targetDatasetId) }}</template>
        </el-table-column>
        <el-table-column label="目标数据字段" min-width="140">
          <template #default="scope">
            <el-space wrap>
              <el-tag v-for="field in (scope.row.targetFields || [])" :key="field" type="info">{{ field }}</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="说明">
          <template #default="scope">{{ scope.row.description }}</template>
        </el-table-column>
      </el-table>
    </el-form>
  </el-drawer>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="80%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <layout-heading title="基础信息" />
      <el-descriptions :column="2" label-width="100px" border>
        <el-descriptions-item label="名称" prop="name" :span="2">
          <el-input v-model="form.name" />
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          <form-input-number v-model="form.sort" />
        </el-descriptions-item>
        <el-descriptions-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-select v-model="form.labels" multiple filterable allow-create :reserve-keyword="false" default-first-option placeholder="输入后回车创建标签" />
        </el-descriptions-item>
        <el-descriptions-item label="授权角色">
          <form-select v-model="form.roleIds" :callback="RoleApi.list" multiple clearable />
        </el-descriptions-item>
        <el-descriptions-item label="描述信息" :span="2">
          <el-input type="textarea" v-model="form.description" />
        </el-descriptions-item>
      </el-descriptions>

      <layout-heading title="数据集" description="通过下拉检索数据集并添加到主题中">
        <template #extra>
          <el-space>
            <form-select v-model="pendingDatasetId" v-model:selected="pendingDataset" :callback="DatasetApi.list"
                         placeholder="输入名称检索数据集" class="dataset-search" />
            <el-button type="primary" @click="handleAddDataset">添加</el-button>
          </el-space>
        </template>
      </layout-heading>
      <el-empty v-if="!form.content.datasetIds.length" description="暂无数据集" />
      <el-table v-else :data="selectedDatasets" :row-key="(record: any) => record.id" :border="true" table-layout="auto" class="mb-15">
        <el-table-column type="expand">
          <template #default="scope">
            <DataSchemaTable v-if="(scope.row.fields || []).length" :model-value="scope.row.fields" class="dataset-fields" />
            <el-empty v-else :image-size="40" description="暂无字段信息" />
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" />
        <el-table-column prop="name" label="数据集" />
        <el-table-column label="服务方式">
          <template #default="scope">{{ scope.row.typeText || '未知' }}</template>
        </el-table-column>
        <el-table-column label="主键">
          <template #default="scope">
            <span v-if="!(scope.row.pks || []).length">暂无</span>
            <el-space v-else wrap>
              <el-tag v-for="item in scope.row.pks" :key="item" type="info">{{ item }}</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="字段数量">
          <template #default="scope">{{ (scope.row.fields || []).length }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="scope">
            <el-button link type="danger" @click="handleRemoveDataset(scope.row.id)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <layout-heading title="字段关联" description="配置数据集字段之间的外键引用关系">
        <template #extra>
          <el-button text @click="handleAddRelation">添加关联</el-button>
        </template>
      </layout-heading>
      <el-empty v-if="!relationRows.length" description="暂无字段关联" />
      <el-table v-else :data="relationRows" :border="true" table-layout="auto">
        <el-table-column label="源数据集" min-width="140">
          <template #default="scope">
            <el-select v-model="scope.row.sourceDatasetId" placeholder="选择源数据集" filterable
                       @change="scope.row.sourceFields = []">
              <el-option v-for="item in selectedDatasets" :key="'s' + item.id" :value="Number(item.id)" :label="item.name" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="源数据字段" min-width="140">
          <template #default="scope">
            <el-select v-model="scope.row.sourceFields" placeholder="请选择源字段" filterable multiple clearable>
              <el-option v-for="field in fieldsOfDataset(scope.row.sourceDatasetId)" :key="field.name" :value="field.name"
                         :label="field.name + (field.title && field.title !== field.name ? ' - ' + field.title : '')" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="目标数据集" min-width="140">
          <template #default="scope">
            <el-select v-model="scope.row.targetDatasetId" placeholder="选择目标数据集" filterable
                       @change="scope.row.targetFields = []">
              <el-option v-for="item in selectedDatasets" :key="'t' + item.id" :value="Number(item.id)" :label="item.name" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="目标数据字段" min-width="140">
          <template #default="scope">
            <el-select v-model="scope.row.targetFields" placeholder="请选择目标字段" filterable multiple clearable>
              <el-option v-for="field in fieldsOfDataset(scope.row.targetDatasetId)" :key="field.name" :value="field.name"
                         :label="field.name + (field.title && field.title !== field.name ? ' - ' + field.title : '')" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="160">
          <template #default="scope">
            <el-input v-model="scope.row.description" placeholder="选填" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="scope">
            <el-button link type="danger" @click="handleRemoveRelation(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
.el-descriptions {
  margin-bottom: 15px;
}
.dataset-search {
  width: 320px;
}
</style>
