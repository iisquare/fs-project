<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import DatasetApi from '@/api/bi/DatasetApi';
import OlapApi from '@/api/bi/OlapApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import RoleApi from '@/api/member/RoleApi';
import DataSchemaTable from '@/components/Data/DataSchemaTable.vue';
import DataFieldSelect from '@/components/Data/DataFieldSelect.vue';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'typeText', label: '服务方式' },
  { prop: 'expression', label: '定时表达式', hide: true },
  { prop: 'content', label: '查询语句', hide: true },
  { prop: 'pks', label: '主键字段', hide: true },
  { prop: 'partitions', label: '分区字段', hide: true },
  { prop: 'labels', label: '标签', slot: 'labels' },
  { prop: 'role', label: '授权角色', slot: 'role' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'lastSyncedTime', label: '同步时间', formatter: DateUtil.render },
])
const config: any = ref({
  ready: false,
  sorts: {},
  status: {},
  types: {},
  fieldTypes: [],
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, roleIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DatasetApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  DatasetApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const triggerLoading = ref<any>(null)
const fieldsLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入数据集名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择服务方式', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})
const handleAdd = () => {
  form.value = {
    status: '1',
    type: 'direct',
    labels: [],
    roleIds: [],
    fields: [],
  }
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
    labels: scope.row.labels || [],
    roleIds: scope.row.roleIds || [],
  })
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    DatasetApi.save(form.value, { success: true }).then(() => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleTrigger = (scope: any) => {
  if (triggerLoading.value) return
  triggerLoading.value = scope.row.id
  DatasetApi.trigger(scope.row.id, { success: true }).then(() => {
    handleRefresh(false, true)
  }).catch(() => {}).finally(() => {
    triggerLoading.value = null
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DatasetApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleLog = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/server/cron/rpcLog',
    query: RouteUtil.filter({ jobName: String(scope.row.id), jobGroup: 'com.iisquare.fs.web.bi.service.DatasetService' })
  })
}
const active = ref('table')
const handleUpdateFields = () => {
  const sql = (form.value.content || '').trim()
  if (!sql) {
    ElMessage.warning('请先填写查询语句')
    return
  }
  if (fieldsLoading.value) return
  fieldsLoading.value = true
  const previousFields: Record<string, any> = {}
  ;(form.value.fields || []).forEach((item: any) => {
    if (item?.name) previousFields[item.name] = item
  })
  OlapApi.query({ sql, limit: 1 }).then((result: any) => {
    const columns = (ApiUtil.data(result) || {}).columns || []
    form.value.fields = columns.map((item: any) => {
      const previous = previousFields[item.name]
      const previousType = previous?.type || ''
      const type = config.value.fieldTypes.includes(previousType)
        ? previousType
        : (item.type || previousType || '')
      return {
        name: item.name,
        type,
        title: previous?.title || item.title || '',
        comment: previous?.comment || item.comment || '',
      }
    })
  }).catch(() => {}).finally(() => {
    fieldsLoading.value = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="服务方式" prop="type">
        <el-select v-model="filters.type" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
        </el-select>
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
        <button-add v-permit="'bi:dataset:add'" @click="handleAdd" />
        <button-delete v-permit="'bi:dataset:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
          <el-space><el-tag v-for="item in scope.row.labels" :key="item">{{ item }}</el-tag></el-space>
        </template>
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button v-if="scope.row.type === 'cron'" link @click="handleTrigger(scope)" :loading="triggerLoading === scope.row.id" v-permit="'bi:dataset:'">触发</el-button>
          <el-button link @click="(e: any) => handleLog(scope, e)" v-permit="'bi:dataset:'">日志</el-button>
          <el-button link @click="handleShow(scope)" v-permit="'bi:dataset:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'bi:dataset:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <layout-heading title="基础信息" />
    <el-descriptions :column="2" label-width="120px" border>
      <el-descriptions-item label="名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="服务方式">{{ form.typeText }}</el-descriptions-item>
      <el-descriptions-item label="定时表达式" :span="2">{{ form.expression || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="最后同步时间" :span="2">
        {{ form.lastSyncedTime ? DateUtil.format(form.lastSyncedTime) : '暂无' }}
      </el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="标签">
        <el-space v-if="form.labels?.length"><el-tag v-for="item in form.labels" :key="item">{{ item }}</el-tag></el-space>
        <span v-else>暂无</span>
      </el-descriptions-item>
      <el-descriptions-item label="授权角色">
        <el-space v-if="form.roles?.length"><el-tag v-for="item in form.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        <span v-else>不限制</span>
      </el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
      <el-descriptions-item label="查询语句" :span="2">
        <code-editor v-model="form.content" mode="sql" :height="120" resizable />
      </el-descriptions-item>
      <el-descriptions-item label="主键字段" :span="2">
        <DataFieldSelect v-if="form.pks?.length" :model-value="form.pks" multiple />
        <span v-else>暂无</span>
      </el-descriptions-item>
      <el-descriptions-item label="分区字段" :span="2">
        <DataFieldSelect v-if="form.partitions?.length" :model-value="form.partitions" multiple />
        <span v-else>暂无</span>
      </el-descriptions-item>
    </el-descriptions>
    <layout-heading title="字段配置" style="margin-top:15px" />
    <DataSchemaTable v-model="form.fields" :types="config.fieldTypes" />
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="60%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-descriptions-item>
        <el-descriptions-item label="服务方式" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
          </el-radio-group>
        </el-descriptions-item>
        <el-descriptions-item label="定时表达式" :span="2">
          <form-cron v-model="form.expression" placeholder="Quartz 格式" />
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-select v-model="form.labels" multiple filterable allow-create :reserve-keyword="false" default-first-option placeholder="输入后回车创建标签" />
        </el-descriptions-item>
        <el-descriptions-item label="授权角色">
          <form-select v-model="form.roleIds" :callback="RoleApi.list" multiple clearable />
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          <form-input-number v-model="form.sort" />
        </el-descriptions-item>
        <el-descriptions-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <el-input type="textarea" v-model="form.description" />
        </el-descriptions-item>
        <el-descriptions-item label="查询语句" :span="2" prop="content">
          <el-alert title="避免非物化视图（直连）数据集间相互查询引用" type="info" show-icon />
          <code-editor v-model="form.content" mode="sql" :height="120" resizable />
        </el-descriptions-item>
        <el-descriptions-item label="主键字段" :span="2">
          <DataFieldSelect v-model="form.pks" v-model:fields="form.fields" editable multiple />
        </el-descriptions-item>
        <el-descriptions-item label="分区字段" :span="2">
          <DataFieldSelect v-model="form.partitions" v-model:fields="form.fields" editable multiple placeholder="请选择分区字段，留空则不分区" />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
    <layout-heading title="字段配置" description="与 SQL 查询字段保持一致，更新字段会保留已填写的名称和注释" style="margin-top:15px">
      <template #extra>
        <el-button text @click="handleUpdateFields" :loading="fieldsLoading">更新字段</el-button>
      </template>
    </layout-heading>
    <el-tabs v-model="active">
      <el-tab-pane label="字段列表" name="table">
        <DataSchemaTable v-model="form.fields" :types="config.fieldTypes" editable />
      </el-tab-pane>
      <el-tab-pane label="字段编辑器" name="schema">
        <DataSchemaText v-model="form.fields" :types="config.fieldTypes" />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<style lang="scss" scoped>
.el-descriptions {
  margin-bottom: 15px;
}
</style>
