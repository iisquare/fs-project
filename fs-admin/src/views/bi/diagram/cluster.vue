<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import RouteUtil from '@/utils/RouteUtil'
import ClusterApi from '@/api/bi/ClusterApi';
import ApiUtil from '@/utils/ApiUtil';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '集群名称' },
  { prop: 'engineText', label: '计算引擎' },
  { prop: 'modeText', label: '运行模式' },
  { prop: 'summary', label: '连接地址' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
])
const config: any = ref({
  ready: false,
  status: {},
  engines: {},
  modes: {},
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
  ClusterApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  ClusterApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
/** 各引擎的提交配置默认值 */
const defaultContent = (engine: string) => ({
  submitType: engine === 'flink' ? 'rest' : 'command',
  rest: '',
  jobmanager: engine === 'flink' ? '127.0.0.1:8081' : '',
  master: engine === 'spark' ? 'spark://127.0.0.1:7077' : '',
  home: engine === 'flink' ? '/opt/flink' : '/opt/spark',
  jar: '',
  entryClass: '',
  args: '',
  parallelism: 0,
  deployMode: 'cluster',
  conf: {},
})
const handleAdd = () => {
  form.value = { engine: 'spark', mode: 'remote', status: '1', sort: 0, description: '', content: defaultContent('spark') }
  formVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: String(scope.row.status),
    content: Object.assign(defaultContent(scope.row.engine), scope.row.content ?? {}),
  })
  formVisible.value = true
}
/** 切换引擎时刷新提交配置默认值，保留用户已填写的通用参数 */
const handleEngineChange = (engine: string) => {
  const base: any = form.value.content ?? {}
  form.value.content = Object.assign(defaultContent(engine), {
    jar: base.jar ?? '',
    rest: base.rest ?? '',
    args: base.args ?? '',
    entryClass: base.entryClass ?? '',
    parallelism: base.parallelism ?? 0,
    conf: base.conf ?? {},
  })
}
const confText = computed({
  get: () => JSON.stringify(form.value.content?.conf ?? {}, null, 2),
  set: (value: string) => {
    try {
      form.value.content.conf = value.trim() ? JSON.parse(value) : {}
    } catch (e) {
      // 非法JSON暂不写回，保留上次有效配置
    }
  },
})
const handleSubmit = () => {
  if (formLoading.value) return
  if (!form.value.name) return ElMessage.warning('请输入集群名称')
  if (!form.value.engine) return ElMessage.warning('请选择计算引擎')
  if (!form.value.mode) return ElMessage.warning('请选择运行模式')
  formLoading.value = true
  ClusterApi.save(form.value, { success: true }).then(() => {
    handleRefresh(false, true)
    formVisible.value = false
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}
const handleTest = (scope: any) => {
  ClusterApi.test({ engine: scope.row.engine, content: scope.row.content }, { success: true })
}
const handleTestForm = () => {
  ClusterApi.test({ engine: form.value.engine, content: form.value.content }, { success: true })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    ClusterApi.delete(ids, { success: true }).then(() => {
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
      <form-search-item label="计算引擎" prop="engine">
        <el-select v-model="filters.engine" clearable placeholder="请选择">
          <el-option v-for="(value, key) in config.engines" :key="key" :value="key" :label="value" />
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
        <button-add v-permit="'bi:cluster:add'" @click="handleAdd" />
        <button-delete v-permit="'bi:cluster:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
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
      <TableColumn :columns="columns" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleTest(scope)" v-permit="'bi:cluster:'" v-if="scope.row.mode === 'remote'">连接测试</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'bi:cluster:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="60%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '集群' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button plain @click="handleTestForm" :disabled="form.mode !== 'remote'">连接测试</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form :model="form" label-width="auto" class="cluster-form">
      <layout-heading title="基础信息" />
      <el-descriptions border :column="2" label-width="110px">
        <el-descriptions-item label="集群名称" :span="2">
          <el-input v-model="form.name" placeholder="如 生产Flink集群" />
        </el-descriptions-item>
        <el-descriptions-item label="计算引擎">
          <el-select v-model="form.engine" placeholder="请选择" filterable @change="handleEngineChange">
            <el-option v-for="(value, key) in config.engines" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="运行模式">
          <el-select v-model="form.mode">
            <el-option v-for="(value, key) in config.modes" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          <el-input-number v-model="form.sort" :controls="false" />
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <el-input type="textarea" v-model="form.description" placeholder="用途说明，选填" />
        </el-descriptions-item>
      </el-descriptions>

      <template v-if="form.mode === 'remote'">
        <layout-heading title="提交配置" description="运行本服务的主机需能读取作业Jar" />
        <el-descriptions border :column="2" label-width="110px">
          <el-descriptions-item label="提交方式">
            <el-select v-model="form.content.submitType">
              <el-option value="command" label="命令行" />
              <el-option value="rest" label="REST接口" />
            </el-select>
          </el-descriptions-item>
          <el-descriptions-item label="并行度">
            <el-input-number v-model="form.content.parallelism" :min="0" :controls="false" placeholder="留空由引擎决定" />
          </el-descriptions-item>
          <el-descriptions-item label="作业Jar路径" :span="2">
            <el-input v-model="form.content.jar" placeholder="运行本服务主机上的绝对路径，如 /opt/jobs/fs-project-thin-flink.jar" />
          </el-descriptions-item>
          <el-descriptions-item label="REST地址">
            <el-input v-model="form.content.rest" :placeholder="form.engine === 'flink' ? 'http://host:8081' : 'http://host:6066'" />
          </el-descriptions-item>
          <el-descriptions-item :label="form.engine === 'flink' ? 'JobManager' : 'Master'">
            <el-input v-if="form.engine === 'flink'" v-model="form.content.jobmanager" placeholder="host:8081" />
            <el-input v-else v-model="form.content.master" placeholder="spark://host:7077 / yarn / k8s://..." />
          </el-descriptions-item>
          <el-descriptions-item label="安装目录">
            <el-input v-model="form.content.home" :placeholder="form.engine === 'flink' ? '/opt/flink' : '/opt/spark'" />
          </el-descriptions-item>
          <el-descriptions-item label="入口类">
            <el-input v-model="form.content.entryClass" placeholder="留空使用默认入口类" />
          </el-descriptions-item>
          <el-descriptions-item label="附加参数" :span="2">
            <el-input v-model="form.content.args" placeholder="追加的程序参数，选填" />
          </el-descriptions-item>
          <el-descriptions-item label="扩展配置" :span="2">
            <el-input
              type="textarea"
              :rows="4"
              v-model="confText"
              placeholder='引擎附加配置，JSON格式，如 {"spark.executor.memory":"2g"}'
            />
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template v-else>
        <layout-heading title="提交配置" />
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="本地执行无需配置集群参数"
          description="作业将在本服务进程内直接执行；需要提交到远程集群时，请将运行模式切换为“远程集群”。"
        />
      </template>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
.cluster-form {
  :deep(.el-descriptions) {
    margin-bottom: 20px;
  }
  :deep(.fs-heading) {
    margin-top: 4px;
  }
  :deep(.el-alert) {
    margin-bottom: 6px;
  }
}
</style>
