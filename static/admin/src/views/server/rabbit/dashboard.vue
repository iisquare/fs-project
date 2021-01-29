<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="">
                <a-input v-model="rabbitURL" placeholder="节点链接地址" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-space>
                <a-button type="primary" @click="connect">{{ filters.state ? '断开' : '连接' }}</a-button>
                <a-button icon="reload" type="primary" :loading="nodeLoading" @click="nodeLoad">重新载入</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form-model>
        <a-tabs v-model="filters.activeTab" @change="tabChange">
          <a-tab-pane tab="节点信息" key="node">
            <a-card title="资源统计">
              <a-form-model layout="inline">
                <a-row :gutter="48">
                  <a-col :md="2" :sm="24"></a-col>
                  <a-col :md="4" :sm="24">
                    <a-form-model-item label="节点数">{{ data.nodeCount }}</a-form-model-item>
                  </a-col>
                  <a-col :md="4" :sm="24">
                    <a-form-model-item label="需求资源">{{ data.requireConsumer }}</a-form-model-item>
                  </a-col>
                  <a-col :md="4" :sm="24">
                    <a-form-model-item label="资源总量">{{ data.maxConsumer }}</a-form-model-item>
                  </a-col>
                  <a-col :md="4" :sm="24">
                    <a-form-model-item label="分配资源">{{ data.usedConsumer }}</a-form-model-item>
                  </a-col>
                  <a-col :md="4" :sm="24">
                    <a-form-model-item label="剩余资源">{{ data.releaseConsumer }}</a-form-model-item>
                  </a-col>
                  <a-col :md="2" :sm="24"></a-col>
                </a-row>
              </a-form-model>
            </a-card>
            <a-divider orientation="left">节点列表</a-divider>
            <a-table
              :columns="nodeColumns"
              :data-source="data.nodes"
              :bordered="true"
              :pagination="false"
              :defaultExpandAllRows="true"
              :rowKey="record => record.id">
              <span slot="id" slot-scope="record">
                <a-icon type="star" :theme="record.leadership ? 'filled' : 'outlined'" />
                <span> - {{ record.id }}</span>
              </span>
              <p slot="expandedRowRender" slot-scope="record" style="margin: 0">
                <a-table
                  :columns="containerColumns"
                  :data-source="record.containers"
                  :bordered="true"
                  :pagination="false"
                  :rowKey="container => container.queueName">
                  <span slot="isRunning" slot-scope="container">
                    <a-icon type="check-circle" v-if="container.isRunning" theme="twoTone" two-tone-color="#52c41a" />
                    <a-icon type="close-circle" v-else />
                  </span>
                </a-table>
              </p>
            </a-table>
          </a-tab-pane>
          <a-tab-pane tab="任务信息" key="task">
            <a-form-model layout="inline">
              <a-form-model-item>
                <a-space>
                  <a-button type="primary" @click="taskAdd">新增</a-button>
                  <a-button type="primary" :loading="taskLoading" @click="taskStartAll">全部启动</a-button>
                  <a-button type="primary" :loading="taskLoading" @click="taskStopAll">全部停止</a-button>
                  <a-button type="primary" :loading="taskLoading" @click="taskRebalance">任务分派</a-button>
                </a-space>
              </a-form-model-item >
            </a-form-model>
            <a-table
              :columns="taskColumns"
              :rowKey="record => record.queueName"
              :dataSource="data.tasks"
              :pagination="false"
              :loading="taskLoading"
              :bordered="true"
            >
              <span slot="nodes" slot-scope="text, record">
                <a-popover>
                  <template slot="content">
                    <p v-for="(v, k) in record.nodes" :key="k">{{ k }} - {{ v }}</p>
                  </template>
                  {{ record.nodeCount }}
                </a-popover>
              </span>
              <span slot="action" slot-scope="text, record">
                <a-button type="link" size="small" v-if="record.status === 'RUNNING'" @click="taskStop(text, record)">停止</a-button>
                <a-button type="link" size="small" v-else @click="taskStart(text, record)">启动</a-button>
                <a-button type="link" size="small" @click="taskEdit(text, record)">编辑</a-button>
                <a-button type="link" size="small" @click="taskRemove(text, record)">删除</a-button>
              </span>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-card>
    <a-modal title="任务编辑" v-model="taskVisible" :confirmLoading="taskLoading" :maskClosable="false" @ok="taskSubmit">
      <a-form-model :model="task" ref="task" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
        <a-form-model-item label="队列名称">
          <a-input v-model="task.queueName" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="处理器">
          <a-input v-model="task.handlerName" auto-complete="on" placeholder="创建后不可修改"></a-input>
        </a-form-model-item>
        <a-form-model-item label="预加载数量">
          <a-input-number v-model="task.prefetchCount" :min="0"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="消费者数量">
          <a-input-number v-model="task.consumerCount" :min="0"></a-input-number>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import DateUtil from '@/utils/date'
import rabbitService from '@/service/server/rabbit'

export default {
  data () {
    return {
      filters: {},
      rabbitURL: '',
      nodeLoading: false,
      data: this.empty(),
      nodeColumns: [
        { title: '名称', scopedSlots: { customRender: 'id' } },
        { title: '状态', dataIndex: 'state' },
        { title: '已分配', align: 'center', dataIndex: 'usedConsumer' },
        { title: '资源总量', align: 'center', dataIndex: 'maxConsumer' },
        { title: '剩余资源', align: 'center', dataIndex: 'releaseConsumer' }
      ],
      containerColumns: [
        { title: '队列名称', dataIndex: 'queueName' },
        { title: '运行状态', align: 'center', scopedSlots: { customRender: 'isRunning' } },
        { title: '预加载数量', align: 'center', dataIndex: 'prefetchCount' },
        { title: '工作者数量', align: 'center', dataIndex: 'consumerCount' },
        { title: '处理类名称', dataIndex: 'handlerName', ellipsis: true }
      ],
      taskLoading: false,
      taskColumns: [
        { title: '队列名称', dataIndex: 'queueName' },
        { title: '运行状态', dataIndex: 'status' },
        { title: '已分配资源', align: 'center', dataIndex: 'nodeConsumer' },
        { title: '节点分布', align: 'center', dataIndex: 'nodeCount', scopedSlots: { customRender: 'nodes' } },
        { title: '预加载数量', align: 'center', dataIndex: 'prefetchCount' },
        { title: '工作者数量', align: 'center', dataIndex: 'consumerCount' },
        { title: '发版日期', dataIndex: 'version', customRender: this.dateRender },
        { title: '处理类名称', dataIndex: 'handlerName', ellipsis: true },
        { title: '操作', width: 170, scopedSlots: { customRender: 'action' } }
      ],
      task: {},
      taskVisible: false
    }
  },
  methods: {
    dateRender (text, record, index) {
      return DateUtil.format(text)
    },
    tabChange () {
      if (!this.filters.state) return
      RouteUtil.filter2query(this, this.filters)
      switch (this.filters.activeTab) {
        case 'node':
          this.nodeLoad()
          break
        case 'task':
          this.nodeLoad()
          break
      }
    },
    empty () {
      return {
        nodeCount: 0,
        maxConsumer: 0,
        usedConsumer: 0,
        releaseConsumer: 0,
        requireConsumer: 0,
        nodes: [],
        tasks: []
      }
    },
    statistic (data) {
      const result = this.empty()
      const queueCount = {}
      const queueNodes = {}
      for (const id in data.nodes) {
        result.nodeCount++
        const node = data.nodes[id]
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
      for (const queueName in data.tasks) {
        const task = data.tasks[queueName]
        task.nodeConsumer = queueCount[queueName] || 0
        task.nodes = queueNodes[queueName] || {}
        task.nodeCount = Object.values(task.nodes).length
        result.tasks.push(task)
        result.requireConsumer += task.consumerCount
      }
      return result
    },
    nodeLoad () {
      this.nodeLoading = true
      rabbitService.taskNodes({ withTask: true, withQueueKey: true }).then(result => {
        if (result.code === 0) {
          this.data = this.statistic(result.data)
          this.nodeLoading = false
          this.taskLoading = false
        }
      })
    },
    connect () {
      this.filters.state = !this.filters.state
      if (this.filters.state) {
        this.rabbitURL = rabbitService.saveURL(this.rabbitURL)
      }
      this.tabChange()
    },
    taskAdd () {
      this.task = { queueName: '', handlerName: '', prefetchCount: 1, consumerCount: 1 }
      this.taskVisible = true
    },
    taskEdit (id, row) {
      this.task = row
      this.taskVisible = true
    },
    taskSubmit () {
      this.taskLoading = true
      rabbitService.taskSubmit(this.task, { success: true }).then((result) => {
        if (result.code === 0) {
          this.taskVisible = false
          this.nodeLoad()
        }
      })
    },
    taskStart (record) {
      this.taskLoading = true
      rabbitService.taskStart(record, { success: true }).then((result) => {
        if (result.code === 0) {
          this.nodeLoad()
        }
      })
    },
    taskStop (record) {
      this.taskLoading = true
      rabbitService.taskStop(record, { success: true }).then((result) => {
        if (result.code === 0) {
          this.nodeLoad()
        }
      })
    },
    taskStartAll () {
      this.taskLoading = true
      rabbitService.taskStartAll({}, { success: true }).then((result) => {
        if (result.code === 0) {
          this.nodeLoad()
        }
      })
    },
    taskStopAll () {
      this.taskLoading = true
      rabbitService.taskStopAll({}, { success: true }).then((result) => {
        if (result.code === 0) {
          this.nodeLoad()
        }
      })
    },
    taskRebalance () {
      this.taskLoading = true
      rabbitService.taskRebalance({}, { success: true }).then((result) => {
        if (result.code === 0) {
          this.nodeLoad()
        }
      })
    },
    taskRemove (id, row) {
      this.taskLoading = true
      rabbitService.taskRemove({ queueName: row.queueName }, { success: true }).then((result) => {
        this.nodeLoad()
      })
    }
  },
  created () {
    this.rabbitURL = rabbitService.baseURL()
    this.filters = RouteUtil.query2filter(this, { state: false, activeTab: 'node' })
  },
  mounted () {
    this.tabChange()
  }
}
</script>
