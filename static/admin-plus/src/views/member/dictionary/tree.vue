<script setup lang="ts">
import { onMounted, ref, h } from 'vue';
import { ElDivider, type FormInstance, type TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import DictionaryApi from '@/api/member/DictionaryApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'name', label: '名称' },
  { prop: 'pinyin', label: '拼音' },
  { prop: 'content', label: '内容' },
  { prop: 'leaf', label: '叶子' },
  { prop: 'id', label: 'ID' },
  { prop: 'ancestorId', label: '祖级' },
  { prop: 'parentId', label: '父级' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config = ref({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, roleIds: [] }))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DictionaryApi.tree(filters.value).then((result: any) => {
    rows.value = result.data
    toggle.value = false
    expandedRowKeys.value = TableUtil.expandedRowKeys(rows.value)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  DictionaryApi.config().then(result => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  })
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  ancestorId: [{ required: true, message: '请选择所属字典', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleAdd = () => {
  form.value = {
    ancestorId: filters.value.ancestorId ?? 0,
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

const ancestorRef = ref()
const handleSublevel = (scope: any) => {
  if (scope.row.ancestorId) {
    form.value = {
      ancestorId: scope.row.ancestorId,
      parentId: scope.row.id,
      status: '1',
    }
    formVisible.value = true
  } else {
    filters.value = {
      ancestorId: scope.row.id
    }
    handleRefresh(true, false)
    ancestorRef.value.reset(scope.row.id)
  }
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    DictionaryApi.save(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
      formVisible.value = false
    })
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DictionaryApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}

const toggle = ref(false)
const expandedRowKeys = ref([])
const spacer = h(ElDivider, { direction: 'vertical' })
const handleExpand = () => {
  toggle.value = !toggle.value
  if (toggle.value) {
    expandedRowKeys.value = []
  } else {
    expandedRowKeys.value = TableUtil.expandedRowKeys(rows.value)
  }
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <template #header>
      <div class="flex-between">
        <el-space :size="0" :spacer="spacer">
          <el-button @click="handleExpand" :icon="expandedRowKeys.length === 0 ? ElementPlusIcons.Expand : ElementPlusIcons.Fold"></el-button>
          <button-add v-permit="'member:menu:add'" @click="handleAdd" />
          <button-delete v-permit="'member:menu:delete'" :disabled="selection.length === 0" @click="handleDelete" />
        </el-space>
        <el-space>
          <el-form ref="filterRef" :model="filters" inline>
            <el-form-item label="字典" prop="ancestorId">
              <form-select ref="ancestorRef" v-model="filters.ancestorId" :callback="DictionaryApi.list" clearable placeholder="默认为祖级字典列表" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleRefresh(true, false)">载入</el-button>
            </el-form-item>
          </el-form>
        </el-space>
      </div>
    </template>
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
        <template #ancestorId="{ row }">[{{ row.ancestorId }}]{{ row.ancestorId > 0 ? row.ancestorInfo?.name : '祖节点' }}</template>
        <template #parentId="{ row }">[{{ row.parentId }}]{{ row.parentId > 0 ? row.parentInfo?.name : '根节点' }}</template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'member:dictionary:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'member:dictionary:modify'">编辑</el-button>
          <el-button link @click="handleSublevel(scope)" v-permit="'member:dictionary:add'">子级</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id">
    <el-form :model="form" label-width="auto">
      <el-form-item label="祖级">{{ form.ancestorId }}</el-form-item>
      <el-form-item label="父级">{{ form.parentId }}</el-form-item>
      <el-form-item label="名称">{{ form.name }}</el-form-item>
      <el-form-item label="拼音">{{ form.pinyin }}</el-form-item>
      <el-form-item label="内容">{{ form.content }}</el-form-item>
      <el-form-item label="叶子">{{ form.leaf }}</el-form-item>
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
      <el-form-item label="祖级" prop="ancestorId">
        <el-input v-model="form.ancestorId" />
      </el-form-item>
      <el-form-item label="父级" prop="parentId">
        <el-input v-model="form.parentId" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="拼音" prop="pinyin">
        <el-input v-model="form.pinyin" />
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <el-input v-model="form.content" />
      </el-form-item>
      <el-form-item label="叶子" prop="leaf">
        <el-checkbox v-model="form.leaf" />
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
