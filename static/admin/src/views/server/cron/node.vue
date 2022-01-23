<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-space class="fs-ui-ctrl">
          <a-button @click="reload()" :loading="loading">重新载入</a-button>
          <a-button @click="restart(false)" :loading="loading" :disabled="selection.selectedRowKeys.length == 0">重新启动</a-button>
          <a-button @click="standby(false)" :loading="loading" :disabled="selection.selectedRowKeys.length == 0">待机等候</a-button>
          <a-button @click="shutdown(false)" :loading="loading" :disabled="selection.selectedRowKeys.length == 0">终止运行</a-button>
          <a-button @click="restart(true)" :loading="loading">重启全部</a-button>
          <a-button @click="standby(true)" :loading="loading">全部待机</a-button>
          <a-button @click="shutdown(true)" :loading="loading">终止全部</a-button>
          <a-checkbox v-model="modeForce">强制模式</a-checkbox>
        </a-space>
        <a-table
          :columns="columns"
          :rowKey="record => record.id"
          :dataSource="rows"
          :pagination="false"
          :loading="loading"
          :bordered="true"
          :rowSelection="selection"
        >
          <span slot="id" slot-scope="record">
            <a-icon type="star" :theme="record.leadership ? 'filled' : 'outlined'" />
            <span> - {{ record.id }}</span>
          </span>
          <span slot="scheduler" slot-scope="record">
            <a-space>
              <a-tag color="orange" v-if="record.scheduler.standby">standby</a-tag>
              <a-tag color="green" v-if="record.scheduler.started">started</a-tag>
              <a-tag color="red" v-if="record.scheduler.shutdown">shutdown</a-tag>
            </a-space>
          </span>
        </a-table>
      </div>
    </a-card>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import cronService from '@/service/server/cron'

export default {
  data () {
    return {
      columns: [
        { title: '节点', scopedSlots: { customRender: 'id' } },
        { title: '状态', dataIndex: 'state' },
        { title: '指令', dataIndex: 'command' },
        { title: '实例', dataIndex: 'scheduler.id' },
        { title: '集群', dataIndex: 'scheduler.name' },
        { title: '调度器', scopedSlots: { customRender: 'scheduler' } }
      ],
      nodes: {},
      loading: false,
      modeForce: false,
      selection: RouteUtil.selection({ type: 'radio' })
    }
  },
  computed: {
    rows () {
      return Object.values(this.nodes)
    }
  },
  methods: {
    standby (bAll) {
      this.loading = true
      cronService.nodeStandby({ nodeId: bAll ? '' : this.selection.selectedRowKeys[0] }).then(() => {
        this.reload(true)
      })
    },
    restart (bAll) {
      this.loading = true
      const nodeId = bAll ? '' : this.selection.selectedRowKeys[0]
      cronService.nodeRestart({ nodeId, modeForce: this.modeForce }).then(() => {
        this.reload(true)
      })
    },
    shutdown (bAll) {
      this.loading = true
      const nodeId = bAll ? '' : this.selection.selectedRowKeys[0]
      cronService.nodeShutdown({ nodeId, modeForce: this.modeForce }).then(() => {
        this.reload(true)
      })
    },
    reload (bKeepSelection = false) {
      if (!bKeepSelection) this.selection.clear()
      this.loading = true
      cronService.nodeStats().then((result) => {
        if (result.code === 0) {
          for (const id in result.data.nodes) {
            result.data.nodes[id].command = result.data.commands[id]
          }
          this.nodes = result.data.nodes
        }
        this.loading = false
      })
    }
  },
  mounted () {
    this.reload()
  }
}
</script>
<style lang="less" scoped>
.fs-ui-ctrl {
  margin-bottom: 20px;
}
</style>
