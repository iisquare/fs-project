<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import DatasourceApi from '@/api/bi/DatasourceApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '数据源名称' },
  { prop: 'typeText', label: '数据源类型' },
  { prop: 'summary', label: '摘要' },
  { prop: 'sort', label: '排序' },
  { prop: 'olapable', label: 'OLAP可用', slot: 'olapable', width: 100 },
  { prop: 'statusText', label: '状态' },
])
const config: any = ref({
  ready: false,
  status: {},
  types: {},
})

const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, modelIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DatasourceApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  DatasourceApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})
const lastTestResult = ref<any>(null)
const handleAdd = () => {
  form.value = {
    status: '1',
    olapable: 0,
    content: {},
  }
  lastTestResult.value = null
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
    olapable: scope.row.olapable ?? 0,
  })
  lastTestResult.value = null
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    DatasourceApi.save(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DatasourceApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleTest = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    DatasourceApi.test(form.value, { success: true }).then((result: any) => {
      lastTestResult.value = result
    }).catch((result: any) => {
      lastTestResult.value = result
    }).finally(() => {
      formLoading.value = false
    })
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="类型" prop="type">
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
        <button-add v-permit="'bi:datasource:add'" @click="handleAdd" />
        <button-delete v-permit="'bi:datasource:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
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
        <template #olapable="scope">
          <el-tag :type="scope.row.olapable ? 'success' : 'info'">{{ scope.row.olapable ? '是' : '否' }}</el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'bi:datasource:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'bi:datasource:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-form :model="form" label-width="auto">
      <layout-heading title="基础信息" />
      <el-descriptions border :column="2" label-width="100px">
        <el-descriptions-item label="名称">{{ form.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ form.typeText }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
        <el-descriptions-item label="配置参数" :span="2">
          <el-checkbox v-model="form.olapable" disabled>作为OLAP数据源</el-checkbox>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
        <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
      </el-descriptions>
      <layout-heading title="连接配置" />
      <el-descriptions border :column="2" label-width="100px" v-if="['mysql', 'doris', 'postgresql'].includes(form.type)">
        <el-descriptions-item label="主机地址">{{ form.content?.host }}</el-descriptions-item>
        <el-descriptions-item label="端口">{{ form.content?.port }}</el-descriptions-item>
        <el-descriptions-item label="数据库名" :span="2">{{ form.content?.database }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ form.content?.username }}</el-descriptions-item>
        <el-descriptions-item label="密码"><form-password v-model="form.content.password" /></el-descriptions-item>
        <el-descriptions-item label="附加参数" :span="2">{{ form.content?.paramsQueryString || '暂无' }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions border :column="2" label-width="100px" v-else-if="form.type === 'elasticsearch'">
        <el-descriptions-item label="地址" :span="2">{{ form.content?.uris }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ form.content?.username }}</el-descriptions-item>
        <el-descriptions-item label="密码"><form-password v-model="form.content.password" /></el-descriptions-item>
      </el-descriptions>
      <el-descriptions border :column="2" label-width="150px" v-else-if="form.type === 'mongodb'">
        <el-descriptions-item label="连接地址" :span="2">{{ form.content?.uri }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ form.content?.username }}</el-descriptions-item>
        <el-descriptions-item label="密码"><form-password v-model="form.content.password" /></el-descriptions-item>
        <el-descriptions-item label="认证库">{{ form.content?.authSource }}</el-descriptions-item>
        <el-descriptions-item label="连接超时(ms)">{{ form.content?.connectTimeout }}</el-descriptions-item>
        <el-descriptions-item label="读取超时(ms)">{{ form.content?.readTimeout }}</el-descriptions-item>
        <el-descriptions-item label="最小连接池">{{ form.content?.minSize }}</el-descriptions-item>
        <el-descriptions-item label="最大连接池">{{ form.content?.maxSize }}</el-descriptions-item>
        <el-descriptions-item label="最大等待时间(ms)" :span="2">{{ form.content?.maxWaitTime }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无配置项" />
    </el-form>
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
      <layout-heading title="基础信息" />
      <el-descriptions border :column="2" label-width="100px">
        <el-descriptions-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-descriptions-item>
        <el-descriptions-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" filterable>
            <el-option v-for="(value, key) in config.types" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          <form-input-number v-model="form.sort" />
        </el-descriptions-item>
        <el-descriptions-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="配置参数" :span="2">
          <el-checkbox v-model="form.olapable">作为OLAP数据源</el-checkbox>
        </el-descriptions-item>
        <el-descriptions-item label="描述信息" :span="2">
          <el-input type="textarea" v-model="form.description" />
        </el-descriptions-item>
      </el-descriptions>
      <layout-heading title="连接配置" description="请根据数据源类型填写对应的连接配置参数">
        <template #extra>
          <el-button @click="handleTest" :loading="formLoading" text>测试连接</el-button>
        </template>
      </layout-heading>
      <el-alert :type="ApiUtil.failed(lastTestResult) ? 'warning' : 'success'" show-icon :title="lastTestResult?.message" :description="lastTestResult?.data" :closable="false" v-if="lastTestResult" />
      <el-descriptions border :column="2" label-width="100px" v-if="['mysql', 'doris', 'postgresql'].includes(form.type)">
        <el-descriptions-item label="主机地址"><el-input v-model="form.content.host" /></el-descriptions-item>
        <el-descriptions-item label="端口"><form-input-number v-model="form.content.port" /></el-descriptions-item>
        <el-descriptions-item label="数据库名" :span="2">
          <el-input v-model="form.content.database" placeholder="数据库名可为空；Postgres建议填写，不指定时默认连接用户同名数据库。" />
        </el-descriptions-item>
        <el-descriptions-item label="用户名"><el-input v-model="form.content.username" /></el-descriptions-item>
        <el-descriptions-item label="密码"><el-input type="password" v-model="form.content.password" show-password /></el-descriptions-item>
        <el-descriptions-item label="附加参数" :span="2">
          <el-input type="textarea" v-model="form.content.paramsQueryString" placeholder="额外的JDBC连接字符串" />
        </el-descriptions-item>
      </el-descriptions>
      <el-descriptions border :column="2" label-width="100px" v-else-if="form.type === 'elasticsearch'">
        <el-descriptions-item label="地址" :span="2"><el-input v-model="form.content.uris" placeholder="例如: http://localhost:9200" /></el-descriptions-item>
        <el-descriptions-item label="用户名"><el-input v-model="form.content.username" /></el-descriptions-item>
        <el-descriptions-item label="密码"><el-input type="password" v-model="form.content.password" show-password /></el-descriptions-item>
      </el-descriptions>
      <el-descriptions border :column="2" label-width="150px" v-else-if="form.type === 'mongodb'">
        <el-descriptions-item label="连接地址" :span="2"><el-input v-model="form.content.uri" placeholder="例如: mongodb://localhost:27017/" /></el-descriptions-item>
        <el-descriptions-item label="用户名"><el-input v-model="form.content.username" placeholder="可选，使用URI内的凭据时可不填" /></el-descriptions-item>
        <el-descriptions-item label="密码"><el-input type="password" v-model="form.content.password" show-password placeholder="可选，使用URI内的凭据时可不填" /></el-descriptions-item>
        <el-descriptions-item label="认证库"><el-input v-model="form.content.authSource" placeholder="admin" /></el-descriptions-item>
        <el-descriptions-item label="连接超时(ms)"><form-input-number v-model="form.content.connectTimeout" :min="0" :step="1000" placeholder="3000" /></el-descriptions-item>
        <el-descriptions-item label="读取超时(ms)"><form-input-number v-model="form.content.readTimeout" :min="0" :step="1000" placeholder="15000" /></el-descriptions-item>
        <el-descriptions-item label="最小连接池"><form-input-number v-model="form.content.minSize" :min="0" placeholder="0" /></el-descriptions-item>
        <el-descriptions-item label="最大连接池"><form-input-number v-model="form.content.maxSize" :min="1" placeholder="100" /></el-descriptions-item>
        <el-descriptions-item label="最大等待时间(ms)" :span="2"><form-input-number v-model="form.content.maxWaitTime" :min="0" :step="100" placeholder="1000" /></el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无配置项" />
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
.el-descriptions {
  margin-bottom: 15px;
}
.el-alert {
  word-break: break-all;
}
</style>
