<template>
  <section>
    <a-card :bordered="false" style="margin-bottom: 15px;">
      <div class="table-page-search-wrapper">
        <a-form-model layout="inline">
          <a-space>
            <a-button type="primary" @click="loadState" :loading="loading">刷新统计</a-button>
            <a-button type="primary" @click="reload" :loading="loading">重载数据</a-button>
          </a-space>
        </a-form-model>
      </div>
    </a-card>
    <a-row :gutter="16">
      <a-col :span="8">
        <a-card title="数据统计" :bordered="false">
          <p>用户数：{{ state.userSize }}</p>
          <p>图像数：{{ state.faceSize }}</p>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="数据状态" :bordered="false">
          <a-checkbox slot="extra" v-model="modeForce">强制模式</a-checkbox>
          <p>触发时间：{{ state.triggerLoading|date }}</p>
          <p>完成时间：{{ state.lastLoading|date }}</p>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="性能监控" :bordered="false">
          <p>最后使用：{{ state.lastCompare|date }}</p>
          <p>对比耗时：{{ state.lastCompareElapsed + 'ms' }}</p>
        </a-card>
      </a-col>
    </a-row>
  </section>
</template>

<script>
import faceService from '@/service/face/face'

export default {
  data () {
    return {
      loading: false,
      state: {},
      modeForce: false
    }
  },
  methods: {
    loadState () {
      if (this.loading) {
        return false
      }
      this.loading = true
      faceService.state().then(result => {
        if (result.code === 0) {
          this.state = result.data
        }
        this.loading = false
      })
    },
    reload () {
      if (this.loading) {
        return false
      }
      this.loading = true
      faceService.reload({ modeForce: this.modeForce }, { success: true }).then(result => {
        this.loading = false
        this.loadState()
      })
    }
  },
  mounted () {
    this.loadState()
  }
}
</script>
