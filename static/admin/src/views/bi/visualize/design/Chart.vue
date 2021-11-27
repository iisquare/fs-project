<template>
  <section>
    <div class="fs-ui-space" v-if="loading"><a-skeleton avatar :paragraph="{ rows: 4 }" /></div>
    <div class="fs-ui-space" v-else-if="!axis"><a-empty description="暂无数据" /></div>
    <chart-table
      v-model="levels"
      :config="config"
      :axis="axis"
      :options="options"
      @drill="drill"
      v-else-if="type === 'Table'" />
    <div class="fs-ui-space" v-else><a-empty description="类型暂不支持，请在大屏中配置" /></div>
  </section>
</template>

<script>
import ApiUtil from '@/utils/api'
import visualizeService from '@/service/bi/visualize'

export default {
  name: 'Chart',
  components: {
    ChartTable: () => import('./ChartTable')
  },
  props: {
    value: { type: Number, default: 0 },
    type: { type: String, required: true },
    config: { type: Object, required: true },
    options: { type: Object, default: () => { return {} } }
  },
  data () {
    return {
      axis: null,
      levels: [],
      loading: false,
      lastPreview: null
    }
  },
  methods: {
    reload () {
    },
    drill () {
      if (this.lastPreview) {
        this.preview(this.lastPreview.datasetId, this.lastPreview.preview, this.levels)
      } else {
        if (this.loading) return false
        this.loading = true
        visualizeService.search({ id: this.value, levels: this.levels }).then((result) => {
          if (ApiUtil.succeed(result)) {
            this.axis = result.data
          }
          this.loading = false
        })
      }
    },
    async preview (datasetId, preview, levels) {
      if (this.loading) return false
      this.loading = true
      this.lastPreview = { datasetId, preview, levels }
      return visualizeService.search(this.lastPreview).then((result) => {
        if (ApiUtil.succeed(result)) {
          this.axis = result.data
          this.levels = result.data.levels
        }
        this.loading = false
        return result
      })
    }
  }
}
</script>

<style lang="less" scoped>
.fs-ui-space {
  padding: 25px;
}
</style>
