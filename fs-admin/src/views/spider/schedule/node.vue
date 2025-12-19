<script setup lang="ts">
import { onMounted, ref } from 'vue';
import NodeApi from '@/api/spider/NodeApi';
import DateUtil from '@/utils/DateUtil';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import ElementUtil from '@/utils/ElementUtil';

const loading = ref(false)
const data: any = ref({
  token: {
    top: {},
    job: {},
  },
  storage: {
    top: {},
  },
  nodes: [],
  spiders: [],
  crawlers: [],
})

const handleRefresh = () => {
  loading.value = true
  NodeApi.stats({}).then((result: any) => {
    Object.assign(data.value, result.data, {
      nodes: Object.values(result.data.nodes),
      spiders: Object.values(result.data.spiders),
      crawlers: Object.values(result.data.crawlers),
    })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh()
})

const handleStart = (nodeId = '') => {
  ElementUtil.confirm(`确定启动'${nodeId ? nodeId : '全部'}'节点？`).then(() => {
    NodeApi.start({ nodeId }, { success: true }).then(() => {
      handleRefresh()
    }).catch(() => {})
  }).catch(() => {})
}
const handleStop = (nodeId = '') => {
  ElementUtil.confirm(`确定停止'${nodeId ? nodeId : '全部'}'节点？`).then(() => {
    NodeApi.stop({ nodeId }, { success: true }).then(() => {
      handleRefresh()
    }).catch(() => {})
  }).catch(() => {})
}
const storageVisible = ref(false)
</script>

<template>
  <el-card :bordered="false" shadow="never" class="mb-20">
    <template #header>
      <div class="flex-between">
        <span>令牌桶</span>
        <el-space>
          <el-button :icon="ElementPlusIcons.CaretRight" circle title="启动全部节点" :loading="loading" @click="handleStart()" />
          <el-button :icon="ElementPlusIcons.SwitchButton" circle title="停止全部节点" :loading="loading" @click="handleStop()" />
          <button-refresh @click="handleRefresh" :loading="loading" />
        </el-space>
      </div>
    </template>
    <el-descriptions>
      <el-descriptions-item label="令牌积压：">{{ data.token.size }}</el-descriptions-item>
      <el-descriptions-item label="下次调度：">{{ DateUtil.format(data.token.top.score) }}</el-descriptions-item>
      <el-descriptions-item label="作业模板：">{{ data.token.job.template?.name }}</el-descriptions-item>
      <el-descriptions-item label="有效计数：">{{ data.token.top.effectiveCount }}</el-descriptions-item>
      <el-descriptions-item label="无效计数：">{{ data.token.top.ineffectiveCount }}</el-descriptions-item>
      <el-descriptions-item label="轮转次数：">{{ data.token.top.rotateCount }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
  <el-card :bordered="false" shadow="never" class="mb-20">
    <template #header>
      <div class="flex-between">
        <span>存储队列</span>
        <el-space>
          <el-switch v-model="storageVisible" inline-prompt active-text="展示详情" inactive-text="隐藏详情" />
        </el-space>
      </div>
    </template>
    <el-descriptions>
      <el-descriptions-item label="队列积压：">{{ data.storage.size }}</el-descriptions-item>
      <el-descriptions-item label="链接地址：">{{ data.storage.top.task?.url }}</el-descriptions-item>
      <el-descriptions-item label="采集节点：">{{ data.storage.top.nodeId }}</el-descriptions-item>
    </el-descriptions>
    <el-input type="textarea" :model-value="JSON.stringify(data.storage, null, 4)" :rows="12" v-if="storageVisible" />
  </el-card>
  <el-card :bordered="false" shadow="never" header="采集端" class="mb-20">
    <el-table
      :data="data.crawlers"
      :border="true"
      v-loading="loading"
      table-layout="auto"
    >
      <el-table-column label="节点" prop="nodeId" />
      <el-table-column label="角色" prop="role" />
      <el-table-column label="调度状态" prop="isRunning" />
      <el-table-column label="调度计数" prop="runningCounter" />
      <el-table-column label="轮询计数" prop="pollingCounter" />
      <el-table-column label="工作池">
        <template #default="scope">{{ scope.row.worker.numActive }} / {{ scope.row.worker.maxTotal }}</template>
      </el-table-column>
      <el-table-column label="请求池">
        <template #default="scope">{{ scope.row.fetcher.numActive }} / {{ scope.row.fetcher.maxTotal }}</template>
      </el-table-column>
      <el-table-column label="启动时间">
        <template #default="scope">{{ DateUtil.format(scope.row.time) }}</template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleStop(scope.row.nodeId)" v-if="scope.row.isRunning">停止</el-button>
          <el-button link @click="handleStart(scope.row.nodeId)" v-else>启动</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
  <el-card :bordered="false" shadow="never" header="服务端" class="mb-20">
    <el-table
      :data="data.spiders"
      :border="true"
      v-loading="loading"
      table-layout="auto"
    >
      <el-table-column label="节点" prop="nodeId" />
      <el-table-column label="角色" prop="role" />
      <el-table-column label="节点状态" prop="state" />
      <el-table-column label="主节点" prop="leadership" />
      <el-table-column label="调度状态" prop="isRunning" />
      <el-table-column label="调度计数" prop="runningCounter" />
      <el-table-column label="工作池">
        <template #default="scope">{{ scope.row.worker.numActive }} / {{ scope.row.worker.maxTotal }}</template>
      </el-table-column>
      <el-table-column label="容器并发">
        <template #default="scope">{{ scope.row.server.activeThreads }} / {{ scope.row.server.maxThreads }}</template>
      </el-table-column>
      <el-table-column label="启动时间">
        <template #default="scope">{{ DateUtil.format(scope.row.time) }}</template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleStop(scope.row.nodeId)" v-if="scope.row.isRunning">停止</el-button>
          <el-button link @click="handleStart(scope.row.nodeId)" v-else>启动</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style lang="scss" scoped>
</style>
