<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="">
                <a-input v-model="crawlerURL" placeholder="节点链接地址" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-button type="primary" @click="connect">{{ filters.state ? '断开' : '连接' }}</a-button>
            </a-col>
          </a-row>
        </a-form-model>
        <a-tabs v-model="filters.activeTab" @change="tabChange">
          <a-tab-pane tab="节点信息" key="node">
            <a-card>
              <template slot="title">
                <a-button icon="reload" type="link" :loading="nodeLoading" @click="nodeLoad" />
                <span> - 通道信息</span>
              </template>
              <a-form-model layout="inline">
                <a-row :gutter="48">
                  <a-col :md="6" :sm="24">
                    <a-form-model-item label="token">{{ channel.size }}</a-form-model-item>
                  </a-col>
                  <a-col :md="6" :sm="24">
                    <a-form-model-item label="overstock">{{ channel.overstock }}</a-form-model-item>
                  </a-col>
                  <a-col :md="6" :sm="24">
                    <a-form-model-item label="top">{{ topDate(null, channel.top, null) }}</a-form-model-item>
                  </a-col>
                </a-row>
              </a-form-model>
            </a-card>
            <a-divider orientation="left">节点列表</a-divider>
            <a-collapse>
              <a-collapse-panel :key="node.id" v-for="node in nodes">
                <template slot="header">
                  <a-icon type="star" :theme="node.leadership ? 'filled' : 'outlined'" />
                  <span> - {{ node.id }}</span>
                </template>
                <a-form-model layout="inline">
                  <a-row :gutter="48">
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="#请求Fetcher">-</a-form-model-item>
                    </a-col>
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="active">{{ node.fetcher.numActive }}</a-form-model-item>
                    </a-col>
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="idle">{{ node.fetcher.numIdle }}</a-form-model-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="48">
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="#线程Worker">-</a-form-model-item>
                    </a-col>
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="active">{{ node.worker.numActive }}</a-form-model-item>
                    </a-col>
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="idle">{{ node.worker.numIdle }}</a-form-model-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="48">
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="#资源Pool">-</a-form-model-item>
                    </a-col>
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="minIdle">{{ node.pool.minIdle }}</a-form-model-item>
                    </a-col>
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="maxIdle">{{ node.pool.maxIdle }}</a-form-model-item>
                    </a-col>
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="maxTotal">{{ node.pool.maxTotal }}</a-form-model-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="48">
                    <a-col :md="6" :sm="24">
                      <a-form-model-item label="#持有Counter">-</a-form-model-item>
                    </a-col>
                    <a-col :md="6" :sm="24" :key="key" v-for="(value, key) in node.counter">
                      <a-form-model-item :label="key">{{ value }}</a-form-model-item>
                    </a-col>
                  </a-row>
                </a-form-model>
              </a-collapse-panel>
            </a-collapse>
          </a-tab-pane>
          <a-tab-pane tab="作业管理" key="schedule">
            <a-form-model layout="inline">
              <a-form-model-item>
                <a-divider type="vertical" />
                <a-button type="primary" :loading="scheduleLoading" @click="scheduleLoad">载入</a-button>
                <a-divider type="vertical" />
                <a-button type="primary" @click="scheduleAdd">新增</a-button>
                <a-divider type="vertical" />
                <a-button type="primary" @click="scheduleExecuteVisible = true">执行</a-button>
              </a-form-model-item >
            </a-form-model>
            <a-table
              :columns="scheduleColumns"
              :rowKey="record => record.id"
              :dataSource="schedules"
              :pagination="false"
              :loading="scheduleLoading"
              :bordered="true"
            >
              <span slot="action" slot-scope="text, record">
                <a-button type="link" size="small" @click="scheduleRun(text, record)">运行</a-button>
                <a-button type="link" size="small" @click="scheduleEdit(text, record)">编辑</a-button>
                <a-button type="link" size="small" @click="scheduleRemove(text, record)">删除</a-button>
              </span>
            </a-table>
          </a-tab-pane>
          <a-tab-pane tab="分组限制" key="group">
            <a-form-model layout="inline">
              <a-form-model-item>
                <a-divider type="vertical" />
                <a-button type="primary" :loading="groupLoading" @click="groupLoad">载入</a-button>
                <a-divider type="vertical" />
                <a-button type="primary" @click="groupAdd">新增</a-button>
              </a-form-model-item >
            </a-form-model>
            <a-table
              :columns="groupColumns"
              :rowKey="record => record.name"
              :dataSource="groups"
              :pagination="false"
              :loading="groupLoading"
              :bordered="true"
            >
              <span slot="action" slot-scope="text, record">
                <a-button type="link" size="small" @click="groupEdit(text, record)">编辑</a-button>
                <a-button type="link" size="small" @click="groupRemove(text, record)">删除</a-button>
              </span>
            </a-table>
          </a-tab-pane>
          <a-tab-pane tab="请求代理" key="proxy">
            <a-form-model layout="inline">
              <a-form-model-item>
                <a-divider type="vertical" />
                <a-button type="primary" :loading="proxyLoading" @click="proxyLoad">载入</a-button>
                <a-divider type="vertical" />
                <a-button type="primary" @click="proxyAdd">新增</a-button>
              </a-form-model-item >
            </a-form-model>
            <a-table
              :columns="proxyColumns"
              :rowKey="record => record.name"
              :dataSource="proxies"
              :pagination="false"
              :loading="proxyLoading"
              :bordered="true"
            >
              <span slot="action" slot-scope="text, record">
                <a-button type="link" size="small" @click="proxyEdit(text, record)">编辑</a-button>
                <a-button type="link" size="small" @click="proxyRemove(text, record)">删除</a-button>
              </span>
            </a-table>
          </a-tab-pane>
          <a-tab-pane tab="历史状态" key="history">
            <a-form-model layout="inline">
              <a-form-model-item>
                <a-divider type="vertical" />
                <a-button type="primary" :loading="historyLoading" @click="historyLoad">载入</a-button>
              </a-form-model-item >
            </a-form-model>
            <a-table
              :columns="historyColumns"
              :rowKey="record => record.scheduleId"
              :dataSource="histories"
              :pagination="false"
              :loading="historyLoading"
              :bordered="true"
            >
              <span slot="action" slot-scope="text, record">
                <a-button type="link" size="small" @click="historyChange(text, record, 'start')" v-if="record.status === 'PAUSE'">唤起</a-button>
                <a-button type="link" size="small" @click="historyChange(text, record, 'pause')" v-if="record.status === 'RUNNING'">暂停</a-button>
                <a-button type="link" size="small" @click="historyChange(text, record, 'stop')" v-if="record.status === 'PAUSE' || record.status === 'RUNNING'">停止</a-button>
                <a-button type="link" size="small" @click="historyClear(text, record)">清空</a-button>
                <a-button type="link" size="small" @click="historyRemove(text, record)" v-if="record.status === 'STOP' || record.status === 'FINISHED'">删除</a-button>
              </span>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-card>
    <!--作业编辑-->
    <a-modal title="作业编辑" v-model="scheduleVisible" :confirmLoading="scheduleLoading" :maskClosable="false" :width="640">
      <a-textarea v-model="scheduleJSON" :autoSize="{ minRows: 8, maxRows: 21 }" />
      <a-form-model layout="inline" slot="footer">
        <a-form-model-item>
          <a-input v-model="templateId" placeholder="模板主键"></a-input>
        </a-form-model-item>
        <a-form-model-item>
          <a-button type="primary" @click="loadTemplate">载入</a-button>
        </a-form-model-item>
        <a-form-model-item>
          <a-button type="primary" @click="scheduleFormat">格式化</a-button>
        </a-form-model-item>
        <a-form-model-item>
          <a-button type="primary" @click="schedulePure">净化</a-button>
        </a-form-model-item>
        <a-form-model-item>
          <a-button @click="scheduleVisible = false">取消</a-button>
        </a-form-model-item>
        <a-form-model-item>
          <a-button type="primary" @click="scheduleSubmit" :loading="scheduleLoading">提交</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--作业执行-->
    <a-modal title="作业执行" v-model="scheduleExecuteVisible" :confirmLoading="scheduleLoading" :maskClosable="false" @ok="scheduleExecute">
      <a-form-model :model="task" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="作业">
          <a-input v-model="task.scheduleId" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="页面">
          <a-input v-model="task.templateKey" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="参数">
          <a-textarea v-model="task.parameters" placeholder="JSON数组字符串" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--分组限制-->
    <a-modal title="分组限制" v-model="groupVisible" :confirmLoading="groupLoading" :maskClosable="false" @ok="groupSubmit">
      <a-form-model :model="group" ref="group" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称">
          <a-input v-model="group.name" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="目标">
          <a-select v-model="group.target" placeholder="请选择应用对象">
            <a-select-option value="broadcast">集群限制</a-select-option>
            <a-select-option value="node">节点限制</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="间隔">
          <a-input-number v-model="group.interval" :min="0" label="并发间隔（毫秒）"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="并发数">
          <a-input-number v-model="group.concurrent" :min="1" label="并发数量"></a-input-number>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--请求代理-->
    <a-modal title="请求代理" v-model="proxyVisible" :confirmLoading="proxyLoading" :maskClosable="false" @ok="proxySubmit">
      <a-form-model :model="proxy" ref="proxy" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称">
          <a-input v-model="proxy.name" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="目标">
          <a-select v-model="proxy.target" mode="combobox" :allowClear="true" placeholder="请选择应用对象">
            <a-select-option value="broadcast">集群限制</a-select-option>
            <a-select-option :value="node.id" :key="node.id" v-for="node in nodes">{{ node.id }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="协议">
          <a-select v-model="proxy.schema" placeholder="请选择请求协议">
            <a-select-option value="HTTP">HTTP</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="主机">
          <a-input v-model="proxy.host" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="端口">
          <a-input-number v-model="proxy.port" :min="1" label="端口"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="连接超时">
          <a-input-number v-model="proxy.connectTimeout" :min="0"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="交互超时">
          <a-input-number v-model="proxy.socketTimeout" :min="0"></a-input-number>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import DateUtil from '@/utils/date'
import crawlerService from '@/service/spider/crawler'
import templateService from '@/service/spider/template'

export default {
  data () {
    return {
      filters: {},
      crawlerURL: '',
      templateId: '',
      nodeLoading: false,
      nodes: {},
      channel: {},
      scheduleLoading: false,
      schedules: [],
      scheduleColumns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '名称', dataIndex: 'name' },
        { title: '分组', dataIndex: 'group' },
        { title: '优先级', dataIndex: 'priority' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      scheduleJSON: '',
      scheduleVisible: false,
      task: {
        scheduleId: '',
        templateKey: '',
        parameters: '[{}]'
      },
      scheduleExecuteVisible: false,
      groupLoading: false,
      groups: [],
      groupColumns: [
        { title: '名称', dataIndex: 'name' },
        { title: '目标', dataIndex: 'target' },
        { title: '间隔', dataIndex: 'interval' },
        { title: '并发数', dataIndex: 'concurrent' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      group: {},
      groupVisible: false,
      proxyLoading: false,
      proxies: [],
      proxyColumns: [
        { title: '名称', dataIndex: 'name' },
        { title: '目标', dataIndex: 'target' },
        { title: '协议', dataIndex: 'schema' },
        { title: '主机', dataIndex: 'host' },
        { title: '端口', dataIndex: 'port' },
        { title: '连接超时', dataIndex: 'connectTimeout' },
        { title: '交互超时', dataIndex: 'socketTimeout' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      proxy: {},
      proxyVisible: false,
      historyLoading: false,
      histories: [],
      historyColumns: [
        { title: '作业', dataIndex: 'scheduleId' },
        { title: '触发', dataIndex: 'dispatch', customRender: this.dateRender },
        { title: '更新', dataIndex: 'version', customRender: this.dateRender },
        { title: '状态', dataIndex: 'status' },
        { title: '线程资源', dataIndex: 'token' },
        { title: '每节点数', dataIndex: 'limit' },
        { title: '通道', dataIndex: 'channel' },
        { title: '最近', dataIndex: 'top', customRender: this.topDate },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      historyVisible: false
    }
  },
  methods: {
    tabChange () {
      if (!this.filters.state) return
      RouteUtil.filter2query(this, this.filters)
      switch (this.filters.activeTab) {
        case 'node':
          this.nodeLoad()
          break
        case 'schedule':
          this.scheduleLoad()
          break
        case 'group':
          this.groupLoad()
          break
        case 'proxy':
          this.proxyLoad()
          break
        case 'history':
          this.historyLoad()
          break
      }
    },
    nodeLoad () {
      this.nodeLoading = true
      crawlerService.nodesState().then(result => {
        if (result.code === 0) {
          this.nodes = result.data.nodes
          this.channel = result.data.channel
          this.nodeLoading = false
        }
      })
    },
    groupAdd () {
      this.group = { name: '', target: '', interval: 0, concurrent: 1 }
      this.groupVisible = true
    },
    groupEdit (id, row) {
      this.group = row
      this.groupVisible = true
    },
    groupSubmit () {
      this.groupLoading = true
      crawlerService.scheduleSave({ type: 'group', content: JSON.stringify(this.group) }, { success: true }).then((result) => {
        if (result.code === 0) {
          this.groupVisible = false
          this.groupLoad()
        }
      })
    },
    groupRemove (id, row) {
      this.groupLoading = true
      crawlerService.scheduleRemove({ type: 'group', id: row.name }, { success: true }).then((result) => {
        this.groupLoad()
      })
    },
    groupLoad () {
      this.groupLoading = true
      crawlerService.scheduleGroup().then((result) => {
        if (result.code === 0) this.groups = result.data
        this.groupLoading = false
      })
    },
    proxyAdd () {
      this.proxy = { target: '', schema: 'HTTP', host: '127.0.0.1', port: 80, connectTimeout: 1000, socketTimeout: 10000 }
      this.proxyVisible = true
    },
    proxyEdit (id, row) {
      this.proxy = row
      this.proxyVisible = true
    },
    proxySubmit () {
      this.proxyLoading = true
      crawlerService.scheduleSave({ type: 'proxy', content: JSON.stringify(this.proxy) }, { success: true }).then((result) => {
        if (result.code === 0) {
          this.proxyVisible = false
          this.proxyLoad()
        }
      })
    },
    proxyRemove (id, row) {
      this.proxyLoading = true
      crawlerService.scheduleRemove({ type: 'proxy', id: row.name }, { success: true }).then((result) => {
        this.proxyLoad()
      })
    },
    proxyLoad () {
      this.proxyLoading = true
      crawlerService.scheduleProxy().then((result) => {
        if (result.code === 0) this.proxies = result.data
        this.proxyLoading = false
      })
    },
    historyRemove (id, row) {
      this.historyLoading = true
      crawlerService.scheduleRemove({ type: 'history', id: row.scheduleId }, { success: true }).then((result) => {
        this.historyLoad()
      })
    },
    historyClear (id, row) {
      this.historyLoading = true
      crawlerService.scheduleClear({ id: row.scheduleId }, { success: true }).then((result) => {
        this.historyLoad()
      })
    },
    historyChange (id, row, cmd) {
      this.historyLoading = true
      crawlerService.scheduleChange({ status: cmd, id: row.scheduleId }, { success: true }).then((result) => {
        this.historyLoad()
      })
    },
    historyLoad () {
      this.historyLoading = true
      crawlerService.scheduleHistory().then((result) => {
        if (result.code === 0) this.histories = result.data
        this.historyLoading = false
      })
    },
    loadTemplate () {
      templateService.plain(this.templateId).then((result) => {
        this.scheduleJSON = result ? JSON.stringify(result, null, 2) : '{}'
      })
    },
    scheduleAdd () {
      this.templateId = ''
      this.scheduleJSON = '{}'
      this.scheduleVisible = true
    },
    scheduleEdit (id, row) {
      this.templateId = row.id || ''
      this.scheduleJSON = JSON.stringify(row, null, 2)
      this.scheduleVisible = true
    },
    scheduleFormat () {
      this.scheduleJSON = JSON.stringify(JSON.parse(this.scheduleJSON), null, 2)
    },
    schedulePure () {
      this.scheduleJSON = JSON.stringify(JSON.parse(this.scheduleJSON))
    },
    scheduleSubmit () {
      this.scheduleLoading = true
      crawlerService.scheduleSave({ type: 'schedule', content: this.scheduleJSON }, { success: true }).then((result) => {
        if (result.code === 0) {
          this.scheduleVisible = false
          this.scheduleLoad()
        }
      })
    },
    scheduleRemove (id, row) {
      this.scheduleLoading = true
      crawlerService.scheduleRemove({ type: 'schedule', id: row.id }, { success: true }).then((result) => {
        this.scheduleLoad()
      })
    },
    scheduleRun (id, row) {
      this.scheduleLoading = true
      crawlerService.scheduleSubmit({ id: row.id }, { success: true }).then((result) => {
        this.scheduleLoading = false
      })
    },
    scheduleExecute () {
      this.scheduleLoading = true
      crawlerService.scheduleExecute(this.task, { success: true }).then((result) => {
        this.scheduleLoading = false
        this.scheduleExecuteVisible = false
      })
    },
    scheduleLoad () {
      this.scheduleLoading = true
      crawlerService.scheduleList().then((result) => {
        if (result.code === 0) this.schedules = result.data
        this.scheduleLoading = false
      })
    },
    topDate (text, record, index) {
      if (!record || typeof record.score === 'undefined') return '-'
      return DateUtil.format(record.score)
    },
    dateRender (text, record, index) {
      return DateUtil.format(text)
    },
    connect () {
      this.filters.state = !this.filters.state
      if (this.filters.state) {
        this.crawlerURL = crawlerService.saveURL(this.crawlerURL)
      }
      this.tabChange()
    }
  },
  created () {
    this.crawlerURL = crawlerService.baseURL()
    this.filters = RouteUtil.query2filter(this, { state: false, activeTab: 'node' })
  },
  mounted () {
    this.tabChange()
  }
}
</script>
