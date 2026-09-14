<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import DateUtil from '@/utils/DateUtil'
import ElementUtil from '@/utils/ElementUtil'
import RabbitApi from '@/api/server/RabbitApi'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'

const activeTab = ref('node')
const nodeTableRef = ref<TableInstance>()
const taskTableRef = ref<TableInstance>()
const taskFormRef = ref<FormInstance>()
const nodeLoading = ref(false)
const taskLoading = ref(false)

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

const statistics = computed(() => [
  { label: '节点数', value: data.value.nodeCount },
  { label: '需求资源', value: data.value.requireConsumer },
  { label: '资源总量', value: data.value.maxConsumer },
  { label: '分配资源', value: data.value.usedConsumer },
  { label: '剩余资源', value: data.value.releaseConsumer }
])

const nodeColumns = ref([
  { prop: 'id', label: '名称', slot: 'id' },
  { prop: 'state', label: '状态', slot: 'state', width: '110px', align: 'center' },
  { prop: 'usedConsumer', label: '已分配', align: 'center' },
  { prop: 'maxConsumer', label: '资源总量', align: 'center' },
  { prop: 'releaseConsumer', label: '剩余资源', align: 'center' }
])

const containerColumns = ref([
  { prop: 'queueName', label: '队列名称' },
  { prop: 'isRunning', label: '运行状态', slot: 'isRunning', width: '100px', align: 'center' },
  { prop: 'prefetchCount', label: '预加载数量', align: 'center' },
  { prop: 'consumerCount', label: '工作者数量', align: 'center' },
  { prop: 'handlerName', label: '处理类名称', slot: 'handlerName', width: '220px' }
])

const taskColumns = ref([
  { prop: 'queueName', label: '队列名称', width: '180px' },
  { prop: 'status', label: '运行状态', slot: 'status', width: '100px', align: 'center' },
  { prop: 'nodeConsumer', label: '已分配资源', align: 'center' },
  { prop: 'nodeCount', label: '节点分布', slot: 'nodes', width: '100px', align: 'center' },
  { prop: 'prefetchCount', label: '预加载数量', align: 'center' },
  { prop: 'consumerCount', label: '工作者数量', align: 'center' },
  { prop: 'version', label: '发版日期', formatter: DateUtil.render, align: 'center' },
  { prop: 'handlerName', label: '处理类名称', slot: 'handlerName', width: '220px' },
  { prop: 'action', label: '操作', slot: 'action', width: '200px', align: 'center' }
])

const taskStatusOf = (status: any): { type: 'success' | 'info' | 'warning', label: string } => {
  switch (status) {
    case 'RUNNING':
      return { type: 'success', label: '运行中' }
    case 'STOP':
      return { type: 'info', label: '已停止' }
    case 'STANDBY':
      return { type: 'warning', label: '待机' }
    default:
      return { type: 'info', label: status ?? '未知' }
  }
}

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
    node.containers = Array.isArray(containers) ? containers : Object.values(containers)
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

const nodeLoad = async () => {
  nodeLoading.value = true
  try {
    const result: any = await RabbitApi.taskNodes({ withTask: true, withQueueKey: true })
    data.value = statistic(result.data)
  } catch (e) {
    // 统一错误提示已由 Api 处理
  } finally {
    nodeLoading.value = false
    taskLoading.value = false
  }
}

const tabChange = () => {
  nodeLoad()
}

onMounted(() => {
  nodeLoad()
})

const task = ref<any>({})
const taskEditing = ref(false)
const taskVisible = ref(false)
const taskRules: any = {
  queueName: [{ required: true, message: '请输入队列名称', trigger: 'blur' }],
  handlerName: [{ required: true, message: '请输入处理器类名', trigger: 'blur' }],
  prefetchCount: [{ required: true, message: '请输入预加载数量', trigger: 'change' }],
  consumerCount: [{ required: true, message: '请输入消费者数量', trigger: 'change' }]
}

const taskAdd = () => {
  taskEditing.value = false
  task.value = { queueName: '', handlerName: '', prefetchCount: 1, consumerCount: 1 }
  taskVisible.value = true
}

const taskEdit = (row: any) => {
  taskEditing.value = true
  task.value = Object.assign({}, row)
  taskVisible.value = true
}

const taskSubmit = () => {
  taskFormRef.value?.validate((valid: boolean) => {
    if (!valid || taskLoading.value) return
    taskLoading.value = true
    RabbitApi.taskSubmit(task.value, { success: true }).then(() => {
      taskVisible.value = false
      return nodeLoad()
    }).catch(() => {}).finally(() => {
      taskLoading.value = false
    })
  })
}

const taskStart = (row: any) => {
  if (taskLoading.value) return
  taskLoading.value = true
  RabbitApi.taskStart(row, { success: true }).then(() => {
    return nodeLoad()
  }).catch(() => {}).finally(() => {
    taskLoading.value = false
  })
}

const taskStop = (row: any) => {
  if (taskLoading.value) return
  taskLoading.value = true
  RabbitApi.taskStop(row, { success: true }).then(() => {
    return nodeLoad()
  }).catch(() => {}).finally(() => {
    taskLoading.value = false
  })
}

const taskStartAll = () => {
  if (taskLoading.value) return
  taskLoading.value = true
  RabbitApi.taskStartAll({}, { success: true }).then(() => {
    return nodeLoad()
  }).catch(() => {}).finally(() => {
    taskLoading.value = false
  })
}

const taskStopAll = () => {
  if (taskLoading.value) return
  ElementUtil.confirm('确认停止全部运行中的任务吗？').then(() => {
    taskLoading.value = true
    RabbitApi.taskStopAll({}, { success: true }).then(() => {
      return nodeLoad()
    }).catch(() => {}).finally(() => {
      taskLoading.value = false
    })
  }).catch(() => {})
}

const taskRebalance = () => {
  if (taskLoading.value) return
  ElementUtil.confirm('确认按当前配置重新分配消费者资源吗？').then(() => {
    taskLoading.value = true
    RabbitApi.taskRebalance({}, { success: true }).then(() => {
      return nodeLoad()
    }).catch(() => {}).finally(() => {
      taskLoading.value = false
    })
  }).catch(() => {})
}

const taskRemove = (row: any) => {
  if (taskLoading.value) return
  ElementUtil.confirm(`确认删除队列「${row.queueName}」吗？`).then(() => {
    taskLoading.value = true
    RabbitApi.taskRemove({ queueName: row.queueName }, { success: true }).then(() => {
      return nodeLoad()
    }).catch(() => {}).finally(() => {
      taskLoading.value = false
    })
  }).catch(() => {})
}
</script>

<template>
  <el-card shadow="never" class="worker-card">
    <el-tabs v-model="activeTab" class="worker-tabs" @tab-change="tabChange">
      <el-tab-pane label="节点信息" name="node">
        <div class="stats-grid">
          <div class="stat-item" v-for="item in statistics" :key="item.label">
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-value">{{ item.value }}</span>
          </div>
        </div>

        <div class="fs-table-toolbar flex-between">
          <el-space>
            <span class="section-title">节点列表</span>
            <el-tag v-if="data.nodeCount" size="small" type="info" effect="plain">
              共 {{ data.nodeCount }} 个节点
            </el-tag>
          </el-space>
          <el-space>
            <button-refresh @click="nodeLoad" :loading="nodeLoading" />
            <TableColumnSetting v-model="nodeColumns" :table="nodeTableRef" />
          </el-space>
        </div>

        <el-table
          ref="nodeTableRef"
          :data="data.nodes"
          :row-key="(record: any) => record.id"
          border
          v-loading="nodeLoading"
          default-expand-all
          table-layout="auto"
          empty-text="暂无节点数据"
        >
          <el-table-column type="expand">
            <template #default="scope">
              <el-table
                v-if="scope.row.containers?.length"
                :data="scope.row.containers"
                :row-key="(container: any) => container.queueName"
                border
                size="small"
                table-layout="auto"
              >
                <TableColumn :columns="containerColumns">
                  <template #isRunning="slot">
                    <el-tag :type="slot.row.isRunning ? 'success' : 'danger'" size="small">
                      {{ slot.row.isRunning ? '运行中' : '已停止' }}
                    </el-tag>
                  </template>
                  <template #handlerName="slot">
                    <el-tooltip :content="slot.row.handlerName" placement="top" :disabled="!slot.row.handlerName">
                      <span class="text-ellipsis">{{ slot.row.handlerName }}</span>
                    </el-tooltip>
                  </template>
                </TableColumn>
              </el-table>
              <el-empty v-else description="暂无消费者" :image-size="50" />
            </template>
          </el-table-column>
          <TableColumn :columns="nodeColumns">
            <template #id="scope">
              <div class="flex-start">
                <LayoutIcon :name="scope.row.leadership ? 'StarFilled' : 'Star'" />
                <span> - {{ scope.row.id }}</span>
              </div>
            </template>
            <template #state="scope">
              <el-tag size="small" effect="plain">{{ scope.row.state }}</el-tag>
            </template>
          </TableColumn>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="任务信息" name="task">
        <div class="fs-table-toolbar flex-between">
          <el-space>
            <button-add @click="taskAdd" />
            <el-button type="primary" :loading="taskLoading" :disabled="!data.tasks.length" @click="taskStartAll">
              全部启动
            </el-button>
            <el-button type="danger" plain :loading="taskLoading" :disabled="!data.tasks.length" @click="taskStopAll">
              全部停止
            </el-button>
            <el-button type="warning" plain :loading="taskLoading" :disabled="!data.tasks.length" @click="taskRebalance">
              任务分派
            </el-button>
          </el-space>
          <el-space>
            <el-tag size="small" type="info" effect="plain">共 {{ data.tasks.length }} 个任务</el-tag>
            <button-refresh @click="nodeLoad" :loading="taskLoading || nodeLoading" />
            <TableColumnSetting v-model="taskColumns" :table="taskTableRef" />
          </el-space>
        </div>

        <el-table
          ref="taskTableRef"
          :data="data.tasks"
          :row-key="(record: any) => record.queueName"
          border
          v-loading="taskLoading || nodeLoading"
          table-layout="auto"
          empty-text="暂无任务数据"
        >
          <TableColumn :columns="taskColumns">
            <template #status="scope">
              <el-tag :type="taskStatusOf(scope.row.status).type" size="small">
                {{ taskStatusOf(scope.row.status).label }}
              </el-tag>
            </template>
            <template #nodes="scope">
              <el-popover placement="top" :width="200" trigger="hover">
                <template #reference>
                  <el-tag
                    size="small"
                    :type="scope.row.nodeCount ? 'success' : 'info'"
                    effect="plain"
                    class="cursor-pointer"
                  >
                    {{ scope.row.nodeCount }}
                  </el-tag>
                </template>
                <div class="pop-title">节点分布</div>
                <template v-if="Object.keys(scope.row.nodes || {}).length">
                  <div class="pop-item" v-for="(value, key) in scope.row.nodes" :key="key">
                    <span class="text-ellipsis">{{ key }}</span>
                    <el-tag size="small" type="info" effect="plain">{{ value }}</el-tag>
                  </div>
                </template>
                <el-empty v-else description="未分配节点" :image-size="40" />
              </el-popover>
            </template>
            <template #handlerName="scope">
              <el-tooltip :content="scope.row.handlerName" placement="top" :disabled="!scope.row.handlerName">
                <span class="text-ellipsis">{{ scope.row.handlerName }}</span>
              </el-tooltip>
            </template>
            <template #action="scope">
              <el-space>
                <el-button
                  v-if="scope.row.status === 'RUNNING'"
                  link
                  type="warning"
                  size="small"
                  :disabled="taskLoading"
                  @click="taskStop(scope.row)"
                >
                  停止
                </el-button>
                <el-button
                  v-else
                  link
                  type="success"
                  size="small"
                  :disabled="taskLoading"
                  @click="taskStart(scope.row)"
                >
                  启动
                </el-button>
                <el-button link type="primary" size="small" :disabled="taskLoading" @click="taskEdit(scope.row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" :disabled="taskLoading" @click="taskRemove(scope.row)">
                  删除
                </el-button>
              </el-space>
            </template>
          </TableColumn>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog
    :title="taskEditing ? '编辑任务' : '新增任务'"
    v-model="taskVisible"
    width="520px"
    :close-on-click-modal="false"
    draggable
    @close="taskLoading = false"
    @closed="taskFormRef?.clearValidate()"
  >
    <el-form ref="taskFormRef" :model="task" :rules="taskRules" label-width="auto">
      <el-form-item label="队列名称" prop="queueName">
        <el-input v-model="task.queueName" :disabled="taskEditing" placeholder="请输入队列名称" clearable />
      </el-form-item>
      <el-form-item label="处理器" prop="handlerName">
        <el-input v-model="task.handlerName" :disabled="taskEditing" placeholder="创建后不可修改" clearable />
      </el-form-item>
      <el-form-item label="预加载数量" prop="prefetchCount">
        <el-input-number v-model="task.prefetchCount" :min="0" :step="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="消费者数量" prop="consumerCount">
        <el-input-number v-model="task.consumerCount" :min="0" :step="1" controls-position="right" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="taskVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskLoading" @click="taskSubmit">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.worker-card {
  .worker-tabs {
    :deep(.el-tabs__content) {
      padding-top: 18px;
    }
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 18px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.text-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.cursor-pointer {
  cursor: pointer;
}

.pop-title {
  margin-bottom: 8px;
  font-weight: 600;
}

.pop-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 3px 0;
}
</style>
