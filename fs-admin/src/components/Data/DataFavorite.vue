<script setup lang="ts">
/**
 * 收藏组件 - 默认展示一个下拉按钮，点击展开收藏列表，按 全部/共享/个人 分类查看。
 * 自己收藏的记录可编辑、删除；点击「应用」将内容回填到 v-model 并通过 apply 事件通知调用端。
 *
 * @v-model {any}              当前待收藏内容（应用收藏时回填），可为字符串或配置对象
 * @prop    {String}  type    - 收藏类型（必填），如 sql-查询语句
 * @prop    {Boolean} text    - 是否以文本按钮展示，默认 false（圆形图标按钮）
 * @event   {String}  apply   - 点击应用时触发，参数：content(收藏内容), item(收藏记录)
 *
 * @example
 * <data-favorite v-model="sql" type="sql" @apply="handleApply" />
 */
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import FavoriteApi from '@/api/member/FavoriteApi'
import ApiUtil from '@/utils/ApiUtil'
import ElementUtil from '@/utils/ElementUtil'
import { useUserStore } from '@/stores/user'
import RouteUtil from '@/utils/RouteUtil'

const model = defineModel<any>()

const props = defineProps({
  type: { type: String, required: true },
  text: { type: Boolean, default: false },
})

const emit = defineEmits<{
  (e: 'apply', content: string, item: any): void
}>()

const user = useUserStore()

const visible = ref(false)
const loading = ref(false)
const rows = ref<any[]>([])
const filters: any = ref({
  type: props.type,
  share: 'all',
  name: '',
})
const pagination = ref(RouteUtil.pagination({ pageSize: 5 }))

const totalPages = computed(() => Math.ceil(pagination.value.total / pagination.value.pageSize) || 0)

const shares = [
  { label: '全部', value: 'all' },
  { label: '共享', value: 'only' },
  { label: '个人', value: 'without' },
]

const isOwn = (item: any) => item.createdUid === user.info.id

const handleRefresh = (keepPage: boolean) => {
  loading.value = true
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  FavoriteApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {
    rows.value = []
  }).finally(() => {
    loading.value = false
  })
}

watch(visible, (value) => {
  if (value) handleRefresh(true)
})

watch(() => filters.value.share, () => {
  if (visible.value) handleRefresh(false)
})

const handleApply = (item: any) => {
  model.value = item.content
  emit('apply', item.content, item)
  visible.value = false
}

const formVisible = ref(false)
const formLoading = ref(false)
const form = ref<any>({})
const formRef: any = ref<FormInstance>()
const selectedFavorite = ref<any>(null)

const fillName = () => {
  if (!selectedFavorite.value) {
    ElMessage.warning('请先在「历史收藏」中选择记录')
    return
  }
  form.value.name = selectedFavorite.value.name
}
const rules = ref({
  name: [{ required: true, message: '请输入收藏名称', trigger: 'blur' }],
})

const openForm = (item: any = {}) => {
  form.value = Object.assign({
    type: props.type,
    name: '',
    content: '',
    description: '',
    sharable: 0,
  }, item)
  formVisible.value = true
}

const stringify = (value: any) => {
  if (value === null || value === undefined) return ''
  if (typeof value === 'string') return value
  return JSON.stringify(value, null, 2)
}

const handleCreate = () => {
  const content = stringify(model.value)
  visible.value = false
  openForm({ content })
}

const handleEdit = (item: any) => {
  openForm(item)
}

const handleDelete = (item: any) => {
  ElementUtil.confirm('确定删除收藏「' + item.name + '」？').then(() => {
    FavoriteApi.delete([item.id], { success: true }).then(() => handleRefresh(true)).catch(() => {})
  }).catch(() => {})
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    FavoriteApi.save(form.value, { success: true }).then(() => {
      formVisible.value = false
      handleRefresh(true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
</script>

<template>
  <div>
    <el-popover v-model:visible="visible" placement="bottom-start" :width="320" trigger="click">
      <template #reference>
        <el-button :circle="!props.text" :text="props.text" title="收藏">
          <LayoutIcon name="Star" />
          <span v-if="props.text">收藏</span>
        </el-button>
      </template>
      <div class="data-favorite">
        <div class="data-favorite__header">
          <div class="data-favorite__toolbar flex-between">
            <el-radio-group v-model="filters.share" size="small">
              <el-radio-button v-for="item in shares" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
            </el-radio-group>
            <el-button size="small" type="primary" text @click="handleCreate">收藏当前</el-button>
          </div>
          <el-input
            v-model="filters.name"
            class="data-favorite__search"
            placeholder="搜索名称"
            clearable
            size="small"
            @keyup.enter="() => handleRefresh(false)"
            @clear="() => handleRefresh(false)">
            <template #prefix>
              <LayoutIcon name="Search" />
            </template>
          </el-input>
        </div>
        <el-scrollbar class="data-favorite__body" v-loading="loading">
          <el-empty v-if="!rows.length && !loading" description="暂无收藏" :image-size="60" />
          <div v-for="item in rows" :key="item.id" class="data-favorite__item">
            <div class="data-favorite__item-main" @click="handleApply(item)">
              <div class="data-favorite__item-top">
                <el-tooltip v-if="item.sharable && isOwn(item)" content="个人共享" placement="top">
                  <span class="data-favorite__item-share">
                    <LayoutIcon name="UserFilled" />
                  </span>
                </el-tooltip>
                <el-tooltip v-else-if="item.sharable" content="共享" placement="top">
                  <span class="data-favorite__item-share">
                    <LayoutIcon name="Share" />
                  </span>
                </el-tooltip>
                <el-tooltip v-else-if="isOwn(item)" content="个人" placement="top">
                  <span class="data-favorite__item-share data-favorite__item-share--own">
                    <LayoutIcon name="User" />
                  </span>
                </el-tooltip>
                <span class="data-favorite__item-label">{{ item.name }}</span>
                <el-dropdown v-if="isOwn(item)" trigger="click" size="small" @click.stop>
                  <el-button size="small" text @click.stop>
                    <LayoutIcon name="More" />
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="handleEdit(item)">
                        <LayoutIcon name="Edit" />
                        <span>编辑</span>
                      </el-dropdown-item>
                      <el-dropdown-item @click="handleDelete(item)">
                        <LayoutIcon name="Delete" />
                        <span>删除</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <div v-if="item.description" class="data-favorite__item-desc" :title="item.description">{{ item.description }}</div>
            </div>
          </div>
        </el-scrollbar>
        <div v-if="pagination.total > 0" class="data-favorite__pagination">
          <span class="data-favorite__total">共 {{ totalPages }} 页</span>
          <el-pagination
            v-model:current-page="pagination.currentPage"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            layout="prev, pager, next"
            size="small"
            @change="handleRefresh(true)" />
        </div>
      </div>
    </el-popover>

    <el-dialog v-model="formVisible" title="个人收藏" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="历史收藏">
          <form-select
            v-model="form.id"
            v-model:selected="selectedFavorite"
            :callback="FavoriteApi.list"
            :parameter="() => { return { type: props.type, share: 'without' } }"
            placeholder="请选择历史收藏进行替换，不选则新建"
            clearable />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入收藏名称">
            <template #suffix>
              <el-tooltip content="填入历史收藏名称" placement="top">
                <span class="data-favorite__name-history" @click="fillName">
                  <LayoutIcon name="Clock" />
                </span>
              </el-tooltip>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="内容">
          <code-editor v-model="form.content" mode="javascript" :height="160" resizable />
        </el-form-item>
        <el-form-item label="共享">
          <el-checkbox v-model="form.sharable" :true-value="1" :false-value="0">共享给他人</el-checkbox>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.data-favorite {
  display: flex;
  flex-direction: column;
}

.data-favorite__header {
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.data-favorite__toolbar {
  margin-bottom: 8px;
}

.data-favorite__search {
  width: 100%;

  :deep(.el-input__wrapper) {
    border-radius: 20px;
    background-color: var(--el-fill-color-light);
    box-shadow: 0 0 0 1px transparent inset;
    transition: box-shadow 0.2s, background-color 0.2s;

    &:hover {
      background-color: var(--el-fill-color);
    }

    &.is-focus {
      background-color: var(--el-bg-color);
      box-shadow: 0 0 0 1px var(--el-color-primary) inset;
    }
  }
}

.data-favorite__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.data-favorite__total {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.data-favorite__body {
  min-height: 80px;
  padding-top: 4px;

  :deep(.el-scrollbar__wrap) {
    max-height: 360px;
  }

  :deep(.el-scrollbar__view) {
    padding-right: 8px;
  }
}

.data-favorite__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 4px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &:last-child {
    border-bottom: none;
  }
}

.data-favorite__item-main:hover .data-favorite__item-label {
  color: var(--el-color-primary);
}

.data-favorite__item-main {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.data-favorite__item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.data-favorite__item-share {
  display: inline-flex;
  align-items: center;
  flex: none;
  color: var(--el-color-success);
}

.data-favorite__item-share--own {
  color: var(--el-text-color-secondary);
}

.data-favorite__item-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.data-favorite__item-desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-favorite__name-history {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: var(--el-text-color-secondary);

  &:hover {
    color: var(--el-color-primary);
  }
}
</style>
