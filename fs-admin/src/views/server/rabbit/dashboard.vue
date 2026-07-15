<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DateUtil from '@/utils/DateUtil'
import RouteUtil from '@/utils/RouteUtil'
import RabbitApi from '@/api/server/RabbitApi'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'

const route = useRoute()
const router = useRouter()

const nodeLoading = ref(false)
const emptyData = () => ({
  nodeCount: 0,
  maxConsumer: 0,
  usedConsumer: 0,
  releaseConsumer: 0,
  requireConsumer: 0,
  nodes: [],
  tasks: []
})

const data = ref(emptyData())

const nodeColumns = ref([
  { prop: 'id', label: '名称', slot: 'id' },
  { prop: 'state', label: '状态' },
  { prop: 'usedConsumer', label: '已分配' },
  { prop: 'maxConsumer', label: '资源总量' },
  { prop: 'releaseConsumer', label: '剩余资源' },
])

const containerColumns = ref([
  { prop: 'queueName', label: '队列名称' },
  { prop: 'isRunning', label: '运行状态', slot: 'isRunning' },
  { prop: 'prefetchCount', label: '预加载数量' },
  { prop: 'consumerCount', label: '工作者数量' },
  { prop: 'handlerName', label: '处理类名称', width: '200px' },
])

const taskLoading = ref(false)
const taskColumns = ref([
  { prop: 'queueName', label: '队列名称' },
  { prop: 'status', label: '运行状态' },
  { prop: 'nodeConsumer', label: '已分配资源' },
  { prop: 'nodeCount', label: '节点分布', slot: 'nodes' },
  { prop: 'prefetchCount', label: '预加载数量' },
  { prop: 'consumerCount', label: '工作者数量' },
  { prop: 'version', label: '发版日期', formatter: DateUtil.render },
  { prop: 'handlerName', label: '处理类名称', width: '200px' },
  { prop: 'action', label: '操作', slot: 'action', width: '170px' },
])

const filters = ref(RouteUtil.query2filter(route, { state: false, activeTab: 'node' }))

const statistic = (raw: any) => {
  const result: any = emptyData()
  const queueCount: any = {}
  const queueNodes: any = {}
  for (const id in raw.nodes) {
    result.nodeCount++
    const node = raw.nodes[id]
    result.maxConsumer += node.maxConsumer
    node.usedConsumer = 0
    const containers = node.containers
    for (const queue in containers) {
      const container = containers[queue]
      node.usedConsumer += container.consumerCount
      queueCount[container.queueName] || (queueCount[container.queueName] = 0)
      queueNodes[container.queueName] || (queueNodes[container.queueName] = {})
      queueCount[container.queueName] += container.consumerCount
      queueNodes[container.queueName][node.id] = container.consumerCount
    }
    node.releaseConsumer = node.maxConsumer - node.usedConsumer
    result.nodes.push(node)
    result.usedConsumer += node.usedConsumer
    result.releaseConsumer = result.maxConsumer - result.usedConsumer
  }
  for (const queueName in raw.tasks) {
    const task = raw.tasks[queueName]
    task.nodeConsumer = queueCount[queueName] || 0
    task.nodes = queueNodes[queueName] || {}
    task.nodeCount = Object.values(task.nodes).length
    result.tasks.push(task)
    result.requireConsumer += task.consumerCount
  }
  return result
}

const nodeLoad = () => {
  nodeLoading.value = true
  RabbitApi.taskNodes({ withTask: true, withQueueKey: true }).then((result: any) => {
    data.value = statistic(result.data)
  }).catch(() => {}).finally(() => {
    nodeLoading.value = false
    taskLoading.value = false
  })
}

const tabChange = () => {
  if (!filters.value.state) return
  RouteUtil.filter2query(route, router, filters.value)
  switch (filters.value.activeTab) {
    case 'node':
      nodeLoad()
      break
    case 'task':
      nodeLoad()
      break
  }
}

onMounted(() => {
  tabChange()
})

const connect = () => {
  filters.value.state = !filters.value.state
  tabChange()
}

const task = ref<any>({})
const taskVisible = ref(false)

const taskAdd = () => {
  task.value = { queueName: '', handlerName: '', prefetchCount: 1, consumerCount: 1 }
  taskVisible.value = true
}

const taskEdit = (row: any) => {
  task.value = Object.assign({}, row)
  taskVisible.value = true
}

const taskSubmit = () => {
  taskLoading.value = true
  RabbitApi.taskSubmit(task.value, { success: true }).then(() => {
    taskVisible.value = false
    nodeLoad()
  }).catch(() => {})
}

const taskStart = (record: any) => {
  taskLoading.value = true
  RabbitApi.taskStart(record, { success: true }).then(() => {
    nodeLoad()
  }).catch(() => {})
}

const taskStop = (record: any) => {
  taskLoading.value = true
  RabbitApi.taskStop(record, { success: true }).then(() => {
    nodeLoad()
  }).catch(() => {})
}

const taskStartAll = () => {
  taskLoading.value = true
  RabbitApi.taskStartAll({}, { success: true }).then(() => {
    nodeLoad()
  }).catch(() => {})
}

const taskStopAll = () => {
  taskLoading.value = true
  RabbitApi.taskStopAll({}, { success: true }).then(() => {
    nodeLoad()
  }).catch(() => {})
}

const taskRebalance = () => {
  taskLoading.value = true
  RabbitApi.taskRebalance({}, { success: true }).then(() => {
    nodeLoad()
  }).catch(() => {})
}

const taskRemove = (row: any) => {
  taskLoading.value = true
  RabbitApi.taskRemove({ queueName: row.queueName }, { success: true }).then(() => {
    nodeLoad()
  }).catch(() => {})
}
</script>

<template>
  <el-card shadow="never">
    <el-form :model="filters">
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item>
            <el-space>
              <el-button type="primary" @click="connect">{{ filters.state ? '断开' : '连接' }}</el-button>
              <el-button type="primary" :loading="nodeLoading" @click="nodeLoad">重新载入</el-button>
            </el-space>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <el-tabs v-model="filters.activeTab" @tab-change="tabChange">
      <el-tab-pane label="节点信息" name="node">
        <el-card shadow="never" class="mb-20">
          <template #header>资源统计</template>
          <el-row :gutter="24">
            <el-col :span="4" :offset="2">
              <span>节点数：{{ data.nodeCount }}</span>
            </el-col>
            <el-col :span="4">
              <span>需求资源：{{ data.requireConsumer }}</span>
            </el-col>
            <el-col :span="4">
              <span>资源总量：{{ data.maxConsumer }}</span>
            </el-col>
            <el-col :span="4">
              <span>分配资源：{{ data.usedConsumer }}</span>
            </el-col>
            <el-col :span="4">
              <span>剩余资源：{{ data.releaseConsumer }}</span>
            </el-col>
          </el-row>
        </el-card>
        <el-divider content-position="left">节点列表</el-divider>
        <el-table
          :data="data.nodes"
          :row-key="(record: any) => record.id"
          :bordered="true"
          v-loading="nodeLoading"
          default-expand-all
          table-layout="auto"
        >
          <el-table-column type="expand">
            <template #default="scope">
              <el-table
                :data="scope.row.containers"
                :row-key="(container: any) => container.queueName"
                :bordered="true"
                table-layout="auto"
              >
                <el-table-column label="运行状态" width="80">
                  <template #default="container">
                    <LayoutIcon :name="container.row.isRunning ? 'CircleCheckFilled' : 'CircleCloseFilled'" />
                  </template>
                </el-table-column>
                <TableColumn :columns="containerColumns" />
              </el-table>
            </template>
          </el-table-column>
          <TableColumn :columns="nodeColumns">
            <template #id="scope">
              <div class="flex-start">
                <LayoutIcon :name="scope.row.leadership ? 'StarFilled' : 'Star'" />
                <span> - {{ scope.row.id }}</span>
              </div>
            </template>
          </TableColumn>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="任务信息" name="task">
        <el-form>
          <el-form-item>
            <el-space>
              <button-add @click="taskAdd" />
              <el-button type="primary" :loading="taskLoading" @click="taskStartAll">全部启动</el-button>
              <el-button type="primary" :loading="taskLoading" @click="taskStopAll">全部停止</el-button>
              <el-button type="primary" :loading="taskLoading" @click="taskRebalance">任务分派</el-button>
            </el-space>
          </el-form-item>
        </el-form>
        <el-table
          :data="data.tasks"
          :row-key="(record: any) => record.queueName"
          :bordered="true"
          v-loading="taskLoading"
          table-layout="auto"
        >
          <TableColumn :columns="taskColumns">
            <template #nodes="scope">
              <el-popover placement="top" :width="200" trigger="hover">
                <template #reference>
                  <span>{{ scope.row.nodeCount }}</span>
                </template>
                <p v-for="(v, k) in scope.row.nodes" :key="k">{{ k }} - {{ v }}</p>
              </el-popover>
            </template>
            <template #action="scope">
              <el-space>
                <el-button link size="small" v-if="scope.row.status === 'RUNNING'" @click="taskStop(scope.row)">停止</el-button>
                <el-button link size="small" v-else @click="taskStart(scope.row)">启动</el-button>
                <el-button link size="small" @click="taskEdit(scope.row)">编辑</el-button>
                <el-button link size="small" @click="taskRemove(scope.row)">删除</el-button>
              </el-space>
            </template>
          </TableColumn>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>
  <el-dialog title="任务编辑" v-model="taskVisible" :close-on-click-modal="false" draggable @close="taskLoading = false">
    <el-form :model="task" label-width="auto">
      <el-form-item label="队列名称">
        <el-input v-model="task.queueName" />
      </el-form-item>
      <el-form-item label="处理器">
        <el-input v-model="task.handlerName" placeholder="创建后不可修改" />
      </el-form-item>
      <el-form-item label="预加载数量">
        <el-input-number v-model="task.prefetchCount" :min="0" />
      </el-form-item>
      <el-form-item label="消费者数量">
        <el-input-number v-model="task.consumerCount" :min="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="taskVisible = false">取消</el-button>
        <el-button type="primary" @click="taskSubmit" :loading="taskLoading">确认</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.mb-20 {
  margin-bottom: 20px;
}
</style>
