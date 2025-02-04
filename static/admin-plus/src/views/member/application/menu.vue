<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import { ElDivider, type FormInstance, type TableInstance } from 'element-plus';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { useRoute, useRouter } from 'vue-router';
import ApplicationApi from '@/api/member/ApplicationApi';
import ApiUtil from '@/utils/ApiUtil';
import MenuApi from '@/api/member/MenuApi';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import UIUtil from '@/utils/UIUtil';

const spacer = h(ElDivider, { direction: 'vertical' })
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const info: any = ref({})

const rows = ref([])
const tableRef = ref<TableInstance>()
const selection = ref([])
const expandedRowKeys = ref([])
const config = ref({
  ready: false,
  status: {},
})
const columns = ref([
  { prop: 'name', label: '名称', slot: 'name' },
  { prop: 'fullName', label: '全称' },
  { prop: 'id', label: 'ID' },
  { prop: 'parentId', label: '父级ID' },
  { prop: 'url', label: '链接',slot: 'url' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])

const handleRefresh = () => {
  loading.value = true
  tableRef.value?.clearSelection()
  const id = route.query.id
  MenuApi.tree({ applicationId: id }).then((result: any) => {
    rows.value = result.data
    if (expandedRowKeys.value.length === 0) {
      toggle.value = false
      expandedRowKeys.value = TableUtil.expandedRowKeys(rows.value)
    }
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const toggle = ref(false)
const handleExpand = () => {
  toggle.value = !toggle.value
  if (toggle.value) {
    expandedRowKeys.value = []
  } else {
    expandedRowKeys.value = TableUtil.expandedRowKeys(rows.value)
  }
}

onMounted(() => {
  handleRefresh()
  ApplicationApi.info(route.query.id).then(result => {
    info.value = ApiUtil.data(result)
  }).catch(() => {})
  MenuApi.config().then(result => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleAdd = () => {
  form.value = {
    parentId: 0,
    applicationId: info.value.id,
    module: info.value.serial,
    status: '1',
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
  })
  formVisible.value = true
}
const handleSublevel = (scope: any) => {
  const record = scope.row
  form.value = {
    parentId: record.id,
    applicationId: record.applicationId,
    status: scope.row.status + '',
  }
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    MenuApi.save(form.value, { success: true }).then(result => {
      handleRefresh()
      formVisible.value = false
    })
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    MenuApi.delete(ids, { success: true }).then(() => {
      handleRefresh()
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search">
    <template #header>
      <div class="flex-between">
        <el-space :size="0" :spacer="spacer">
          <el-button @click="handleExpand" :icon="expandedRowKeys.length === 0 ? ElementPlusIcons.Expand : ElementPlusIcons.Fold"></el-button>
          <button-add v-permit="'member:menu:add'" @click="handleAdd" />
          <button-delete v-permit="'member:menu:delete'" :disabled="selection.length === 0" @click="handleDelete" />
        </el-space>
        <el-space :size="0" :spacer="spacer">
          <el-button :icon="ElementPlusIcons.Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
          <el-button @click="router.go(-1)">返回</el-button>
        </el-space>
      </div>
    </template>
    <el-skeleton v-if="loading" />
    <el-descriptions title="应用信息" v-else>
      <el-descriptions-item label="ID">{{ info.id }}</el-descriptions-item>
      <el-descriptions-item label="标识">{{ info.serial }}</el-descriptions-item>
      <el-descriptions-item label="名称">{{ info.name }}</el-descriptions-item>
    </el-descriptions>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="record => record.id"
      :expand-row-keys="expandedRowKeys"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="newSelection => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #url="scope">
          <LayoutIcon :name="scope.row.icon" />
          <RouterLink :to="scope.row.url" :title="scope.row.url" target="_blank">{{ scope.row.target ? scope.row.target : (scope.row.url ? '_self' : '无链接') }}</RouterLink>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'member:menu:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'member:menu:modify'">编辑</el-button>
          <el-button link @click="handleSublevel(scope)" v-permit="'member:menu:add'">子级</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id">
    <el-form :model="form" label-width="auto">
      <el-form-item label="父级">{{ form.parentId }}</el-form-item>
      <el-form-item label="名称">{{ form.name }}</el-form-item>
      <el-form-item label="全称">{{ form.fullName }}</el-form-item>
      <el-form-item label="图标"><LayoutIcon :name="form.icon" />{{ form.icon }}</el-form-item>
      <el-form-item label="链接">{{ form.url }}</el-form-item>
      <el-form-item label="打开方式">{{ form.target }}</el-form-item>
      <el-form-item label="排序">{{ form.sort }}</el-form-item>
      <el-form-item label="状态">{{ form.statusText }}</el-form-item>
      <el-form-item label="描述">{{ form.description }}</el-form-item>
      <el-form-item label="创建者">{{ form.createdUserInfo?.name }}</el-form-item>
      <el-form-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-form-item>
      <el-form-item label="修改者">{{ form.updatedUserInfo?.name }}</el-form-item>
      <el-form-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-form-item>
    </el-form>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-form-item label="ID" prop="id">
        <el-input v-model="form.id" />
      </el-form-item>
      <el-form-item label="应用" prop="applicationId">
        <el-input v-model="form.applicationId" />
      </el-form-item>
      <el-form-item label="父级" prop="parentId">
        <el-input v-model="form.parentId" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="图标" prop="icon">
        <el-input v-model="form.icon" />
      </el-form-item>
      <el-form-item label="链接" prop="url">
        <el-input v-model="form.url" />
      </el-form-item>
      <el-form-item label="打开方式" prop="target">
        <el-autocomplete v-model="form.target" :fetch-suggestions="query => UIUtil.filterSuggestions(MenuApi.target(), query)">
          <template #default="{ item }">{{ item.value }} - {{ item.description }}</template>
        </el-autocomplete>
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" placeholder="请选择">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input type="textarea" v-model="form.description" />
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
