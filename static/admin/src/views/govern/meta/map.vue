<template>
  <section>
    <a-card :bordered="false">
      <a-form-model layout="inline">
        <a-form-model-item label="元数据">
          <a-input-search v-model="keyword" placeholder="" enter-button="检索" @search="search" />
        </a-form-model-item>
      </a-form-model>
    </a-card>
    <a-card :bordered="false" style="margin-top: 25px;">
      <a-row :gutter="25">
        <a-col :sm="6" :xs="24">
          <a-statistic title="模型数量" :value="statistic.modelCount" />
        </a-col>
        <a-col :sm="6" :xs="24">
          <a-statistic title="关系数量" :value="statistic.relationCount" />
        </a-col>
        <a-col :sm="6" :xs="24">
          <a-statistic title="模型关系" :value="statistic.relationModelCount" />
        </a-col>
        <a-col :sm="6" :xs="24">
          <a-statistic title="字段关系" :value="statistic.relationColumnCount" />
        </a-col>
      </a-row>
    </a-card>
    <a-row :gutter="25" style="margin-top: 25px;">
      <a-col :sm="8" :xs="24">
        <a-card title="关系类型" class="fs-ui-card">
          <a-skeleton v-if="!statistic.ready" />
          <a-empty v-else-if="statistic.relations.length === 0" />
          <div ref="relation" class="fs-ui-chart" v-show="statistic.relations.length !== 0"></div>
        </a-card>
      </a-col>
      <a-col :sm="8" :xs="24">
        <a-card title="模型出度" class="fs-ui-card">
          <a-skeleton v-if="!statistic.ready" />
          <a-empty v-else-if="statistic.modelSources.length === 0" />
          <a-list :data-source="statistic.modelSources" v-else>
            <a-list-item slot="renderItem" slot-scope="item">
              <a-list-item-meta :title="`${item.sourceCatalog}${item.sourceModel}`" />
              <div>{{ item.ct }}</div>
            </a-list-item>
          </a-list>
        </a-card>
      </a-col>
      <a-col :sm="8" :xs="24">
        <a-card title="模型入度" class="fs-ui-card">
          <a-skeleton v-if="!statistic.ready" />
          <a-empty v-else-if="statistic.modelTargets.length === 0" />
          <a-list :data-source="statistic.modelTargets" v-else>
            <a-list-item slot="renderItem" slot-scope="item">
              <a-list-item-meta :title="`${item.targetCatalog}${item.targetModel}`" />
              <div>{{ item.ct }}</div>
            </a-list-item>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
    <a-row :gutter="25" style="margin-top: 25px;">
      <a-col :sm="8" :xs="24">
        <a-card title="字段类型" class="fs-ui-card">
          <a-skeleton v-if="!statistic.ready" />
          <a-empty v-else-if="statistic.relations.length === 0" />
          <div ref="type" class="fs-ui-chart" v-show="statistic.relations.length !== 0"></div>
        </a-card>
      </a-col>
      <a-col :sm="8" :xs="24">
        <a-card title="字段出度" class="fs-ui-card">
          <a-skeleton v-if="!statistic.ready" />
          <a-empty v-else-if="statistic.columnSources.length === 0" />
          <a-list :data-source="statistic.columnSources" v-else>
            <a-list-item slot="renderItem" slot-scope="item">
              <a-list-item-meta :title="`${item.sourceCatalog}${item.sourceModel}/${item.sourceColumn}`" />
              <div>{{ item.ct }}</div>
            </a-list-item>
          </a-list>
        </a-card>
      </a-col>
      <a-col :sm="8" :xs="24">
        <a-card title="字段入度" class="fs-ui-card">
          <a-skeleton v-if="!statistic.ready" />
          <a-empty v-else-if="statistic.columnTargets.length === 0" />
          <a-list :data-source="statistic.columnTargets" v-else>
            <a-list-item slot="renderItem" slot-scope="item">
              <a-list-item-meta :title="`${item.targetCatalog}${item.targetModel}/${item.targetColumn}`" />
              <div>{{ item.ct }}</div>
            </a-list-item>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </section>
</template>

<script>
import * as echarts from 'echarts'
import metaService from '@/service/govern/meta'

export default ({
  data () {
    return {
      keyword: '',
      statistic: {
        ready: false,
        modelCount: 0,
        relationCount: 0,
        relationModelCount: 0,
        relationColumnCount: 0,
        relations: [],
        types: [],
        modelSources: [],
        columnSources: [],
        modelTargets: [],
        columnTargets: []
      }
    }
  },
  methods: {
    search () {
      this.$router.push({ path: '/govern/meta/list', query: { keyword: this.keyword } })
    },
    reload () {
      metaService.statistic().then((result) => {
        this.statistic.ready = true
        if (result.code === 0) {
          Object.assign(this.statistic, result.data)
          this.renderRelation()
          this.renderType()
        }
      })
    },
    renderType () {
      echarts.init(this.$refs.type).setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          top: '3%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          boundaryGap: [0, 0.01]
        },
        yAxis: {
          type: 'category',
          data: this.statistic.types.map(item => item.type).reverse()
        },
        series: [{
          name: 'total',
          type: 'bar',
          data: this.statistic.types.map(item => item.ct).reverse()
        }]
      })
    },
    renderRelation () {
      echarts.init(this.$refs.relation).setOption({
        legend: {
          orient: 'vertical',
          x: 'right',
          y: 'top'
        },
        series: [{
          type: 'pie',
          center: ['35%', '50%'],
          roseType: 'area',
          itemStyle: { borderRadius: 5 },
          data: this.statistic.relations.map(item => { return { name: item.relation, value: item.ct } })
        }]
      })
    }
  },
  mounted () {
    this.reload()
  }
})
</script>
<style lang="less" scoped>
.fs-ui-card {
  & /deep/ .ant-card-body {
    height: 300px;
  }
  & /deep/ .ant-empty {
    margin-top: 35px;
  }
  .fs-ui-chart {
    width: 390px;
    height: 250px;
  }
}
</style>
