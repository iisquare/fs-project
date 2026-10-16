<script setup lang="ts">
/**
 * 模型对比 - 历史记录列表（工作台右列，直接铺开，不藏抽屉）。
 *
 * 一次提交就是一条：列表只带摘要（几个完成、几个失败、几个中断），
 * 点一行才去取详情，把当时那组配置与输出整条载入编辑器。
 * 历史与工作区不硬关联，所以恢复只落到草稿上，要留成工作区得显式保存。
 *
 * 检索、分页与刷新都交给父级发请求：组件只负责把关键词与页码抛上去，
 * 免得「当前在第几页」这种状态在两个地方各存一份。
 * 列表不会自己刷新——进行中的一轮靠「刷新」手动取最新摘要，要不要看由用户决定。
 *
 * @prop  {Array}   runs     - 当前页的记录
 * @prop  {Number}  total    - 记录总数（决定分页器显不显示）
 * @prop  {Number}  page     - 当前页码
 * @prop  {Number}  pageSize - 每页条数
 * @prop  {Boolean} loading  - 请求中
 * @emits search             - 按关键词检索（空串表示清空条件）
 * @emits page               - 翻页
 * @emits restore / stop / remove / clear / refresh
 */
import { ref } from 'vue'
import { Delete, Refresh, Search, VideoPause } from '@element-plus/icons-vue'
import DateUtil from '@/utils/DateUtil'

const {
  runs = [],
  total = 0,
  page = 1,
  pageSize = 20,
  loading = false,
} = defineProps<{
  runs?: any[]
  total?: number
  page?: number
  pageSize?: number
  loading?: boolean
}>()
const emit = defineEmits<{
  search: [keyword: string]
  page: [page: number]
  restore: [run: any]
  stop: [run: any]
  remove: [run: any]
  clear: []
  refresh: []
}>()

const keyword = ref('')
const handleSearch = () => emit('search', String(keyword.value ?? '').trim())
const handleClearKeyword = () => {
  keyword.value = ''
  emit('search', '')
}

/**
 * 摘要文案：状态 0 是「进行中」——本轮还有模型的流没结束，
 * 还没有完整摘要（一个模型都没回来时摘要就是空的），这时直接标进行中。
 */
const runSummary = (run: any) => {
  const summary = String(run?.summary ?? '')
  if (summary) return summary
  return 0 === Number(run?.status) ? '进行中' : '无结果'
}
</script>

<template>
  <div class="side-panel">
    <div class="panel-head">
      <span class="panel-title">历史记录（{{ total }}）</span>
      <div class="panel-actions">
        <!-- 进行中的一轮要手动取最新摘要：列表不自动刷新 -->
        <el-button class="panel-refresh" link :icon="Refresh" title="刷新列表" @click="emit('refresh')" />
        <!-- 清空用纯文字按钮：破坏性动作，图标反而要多认一次 -->
        <el-button class="panel-clear" link :disabled="!total" @click="emit('clear')">清空</el-button>
      </div>
    </div>
    <!-- 检索输入内容：标题存的就是用户输入，所以搜标题即搜输入 -->
    <div class="panel-search">
      <el-input
        v-model="keyword"
        size="small"
        clearable
        :prefix-icon="Search"
        placeholder="搜索输入内容"
        @keydown.enter="handleSearch"
        @clear="handleClearKeyword" />
    </div>
    <el-scrollbar class="panel-body" v-loading="loading">
      <div class="panel-inner">
        <div
          class="side-item"
          :class="{ 'is-running': 0 === Number(run.status) }"
          :key="run.id"
          v-for="run in runs"
          @click="emit('restore', run)">
          <div class="item-main">
            <div class="item-title">
              <span class="item-name" :title="run.title">{{ run.title || '未填写输入' }}</span>
            </div>
            <div class="item-meta">
              <span>{{ DateUtil.format(run.createdTime) }}</span>
              <span>{{ run.modelCount || 0 }} 个模型</span>
              <span :class="{ 'is-failed': 2 === Number(run.status), 'is-running': 0 === Number(run.status) }">{{ runSummary(run) }}</span>
            </div>
          </div>
          <div class="item-actions">
            <!-- 进行中的一轮可以直接终止：模型卡死、输出失控时，这是最顺手的收口入口 -->
            <el-button
              class="item-stop"
              v-if="0 === Number(run.status)"
              link
              :icon="VideoPause"
              title="终止本轮"
              @click.stop="emit('stop', run)" />
            <el-button class="item-delete" link :icon="Delete" title="删除该记录" @click.stop="emit('remove', run)" />
          </div>
        </div>
        <el-empty
          :description="keyword ? '没有匹配的记录' : '还没有调试记录'"
          :image-size="52"
          v-if="!runs.length" />
      </div>
    </el-scrollbar>
    <!-- 分页常驻：位置固定，条数变化时不会忽隐忽现 -->
    <div class="panel-foot">
      <el-pagination
        size="small"
        layout="prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        :pager-count="5"
        @current-change="(value: number) => emit('page', value)" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.side-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.panel-head {
  @include flex-between();
  flex: none;
  gap: 8px;
  padding: 10px 12px 6px;
  .panel-title {
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }
  .panel-actions {
    @include flex-start();
    flex: none;
    gap: 2px;
  }
  /* 清空是破坏性操作，划过转红，与删除、反馈的「踩」同一个口径 */
  .panel-clear:hover {
    color: var(--el-color-danger);
  }
}
.panel-search {
  flex: none;
  padding: 4px 10px 10px;
}
.panel-body {
  flex: 1;
  min-height: 0;
}
.panel-inner {
  padding: 0 10px 10px;
}
/* 分页器：窄列里只留上一页 / 页码 / 下一页 */
.panel-foot {
  @include flex-center();
  flex: none;
  padding: 6px 8px 10px;
  /* element-plus 在 .el-pagination 上自己声明了 --el-pagination-bg-color: 白色，
     上一页 / 下一页就是靠它刷底的，容器不设背景也盖不住，必须改到组件这一层 */
  :deep(.el-pagination) {
    --el-pagination-bg-color: transparent;
    --el-pagination-button-disabled-bg-color: transparent;
  }
}
.side-item {
  @include flex-between();
  gap: 6px;
  padding: 6px 8px;
  border-radius: 6px;
  border: solid 1px transparent;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
  & + .side-item {
    margin-top: 4px;
  }
  &:hover {
    background: var(--el-bg-color);
    .item-actions {
      opacity: 1;
    }
  }
  .item-main {
    min-width: 0;
    .item-title {
      @include flex-start();
      gap: 6px;
      .item-name {
        min-width: 0;
        font-size: 13px;
        color: var(--el-text-color-regular);
        @include text-wrap();
      }
    }
    .item-meta {
      @include flex-wrap();
      gap: 2px 10px;
      margin-top: 3px;
      font-size: 12px;
      color: var(--el-text-color-placeholder);
      /* 有失败或中断的那一轮标红，扫一眼就能定位要回看的记录 */
      .is-failed {
        color: var(--el-color-danger);
      }
      /* 进行中的一轮标黄：与「已中断」的警示色区分开，不必点开就知道它还没结束 */
      .is-running {
        color: var(--el-color-warning);
      }
    }
  }
  .item-actions {
    flex: none;
    opacity: 0;
    transition: opacity 0.2s;
  }
  /* 进行中的一轮：终止按钮一直露着，不用先划过才知道能停 */
  &.is-running .item-actions {
    opacity: 1;
  }
  /* 删除默认保持中性，鼠标划过才转红，跟反馈的「踩」一个口径。
     选择器带上 .side-item 是为了压过 element-plus 的 .el-button.is-link:hover */
  .item-delete:hover {
    color: var(--el-color-danger);
  }
  /* 终止同理：划过转黄，与「进行中」的状态色一致 */
  .item-stop:hover {
    color: var(--el-color-warning);
  }
}
</style>
