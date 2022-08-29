<template>
  <section>
    <a-card :bordered="false">
      <a-form-model ref="filters" :model="filters" layout="inline">
        <a-form-model-item label="包">
          <a-input v-model="filters.catalog" placeholder="所属包" :allowClear="true" />
        </a-form-model-item>
        <a-form-model-item label="模型">
          <a-input v-model="filters.model" placeholder="所属模型" :allowClear="true" />
        </a-form-model-item>
        <a-form-model-item label="字段">
          <a-input v-model="filters.code" placeholder="字段编码" :allowClear="true" />
        </a-form-model-item>
        <a-form-model-item label="关系深度">
          <a-input-number v-model="filters.minHops" placeholder="最小深度" :min="0" :allowClear="true" />
          <span> - </span>
          <a-input-number v-model="filters.maxHops" placeholder="最大深度" :min="0" :allowClear="true" />
        </a-form-model-item>
        <a-form-model-item>
          <a-space>
            <a-button type="primary" @click="search(true)" :loading="loading">分析</a-button>
            <a-button @click.native="$router.go(-1)">返回</a-button>
          </a-space>
        </a-form-model-item>
      </a-form-model>
    </a-card>
    <a-card class="fs-ui-left">
      <div ref="relation" class="fs-ui-relation"></div>
    </a-card>
    <a-card title="属性信息" class="fs-ui-right">
      <template slot="extra">
        <a-button-group v-if="selected === null">
          <a-button type="link" @click.native="$router.go(-1)">返回</a-button>
        </a-button-group>
        <a-button-group v-else-if="selected.shape === 'er-edge'">
          <a-button type="link" @click="e => forward('trace', e)">溯源</a-button>
          <a-button type="link" @click="e => forward('cite', e)">引用</a-button>
          <a-button type="link" @click="e => forward('detail', e)">详情</a-button>
        </a-button-group>
        <a-button-group v-else>
          <a-button type="link" @click="e => forward('blood', e)">血缘</a-button>
          <a-button type="link" @click="e => forward('detail', e)">详情</a-button>
        </a-button-group>
      </template>
      <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" v-if="selected === null">
        <a-form-model-item label="模型数量">{{ Object.values(influence.models).length }}</a-form-model-item>
        <a-form-model-item label="关系数量">{{ Object.values(influence.relations).length }}</a-form-model-item>
      </a-form-model>
      <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" v-else-if="selected.shape === 'er-edge'">
        <a-form-model-item label="来源包">{{ selected.data.properties.source_catalog }}</a-form-model-item>
        <a-form-model-item label="来源模型">{{ selected.data.properties.source_model }}</a-form-model-item>
        <a-form-model-item label="来源字段">{{ selected.data.properties.source_column }}</a-form-model-item>
        <a-form-model-item label="关联关系">{{ selected.data.properties.relation }}</a-form-model-item>
        <a-form-model-item label="目标包">{{ selected.data.properties.target_catalog }}</a-form-model-item>
        <a-form-model-item label="目标模型">{{ selected.data.properties.target_model }}</a-form-model-item>
        <a-form-model-item label="目标字段">{{ selected.data.properties.target_column }}</a-form-model-item>
      </a-form-model>
      <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" v-else>
        <a-form-model-item label="所属包">{{ selected.data.catalog }}</a-form-model-item>
        <a-form-model-item label="模型编码">{{ selected.data.code }}</a-form-model-item>
        <a-form-model-item label="模型名称">{{ selected.data.name }}</a-form-model-item>
        <a-form-model-item label="模型类型">{{ selected.data.type }}</a-form-model-item>
        <a-form-model-item label="模型主键">{{ selected.data.pk }}</a-form-model-item>
        <a-form-model-item label="模型描述">{{ selected.data.description }}</a-form-model-item>
      </a-form-model>
    </a-card>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import metaService from '@/service/govern/meta'
import ER from '@/components/X6/er'

export default {
  data () {
    return {
      filters: {},
      loading: false,
      er: null,
      selected: null,
      influence: { nodes: {}, relations: {}, models: {} }
    }
  },
  methods: {
    search (filter2query) {
      this.loading = true
      filter2query && this.$router.push({ path: this.$route.path, query: this.filters }).catch(() => {})
      metaService.influence(this.filters).then(result => {
        if (result.code === 0) {
          this.influence = result.data
          this.renderInfluence()
        }
        this.loading = false
      })
    },
    renderInfluence () {
      this.er.reset(this.er.fromGovernMetaInfluence(this.influence))
      this.er.fitting()
    },
    forward (uri, e) {
      if (!this.selected) return
      if (this.selected.shape === 'er-edge') { // 关系
        if (uri === 'detail') { // 关系列表
          RouteUtil.forward(this, e, {
            path: '/govern/meta/modelRelation',
            query: RouteUtil.filter({
              sourceCatalog: this.selected.data.properties.source_catalog,
              sourceModel: this.selected.data.properties.source_model,
              sourceColumn: this.selected.data.properties.source_column,
              targetCatalog: this.selected.data.properties.target_catalog,
              targetModel: this.selected.data.properties.target_model,
              targetColumn: this.selected.data.properties.target_column
            })
          })
        } else if (uri === 'trace') {
          Object.assign(this.filters, {
            catalog: this.selected.data.properties.source_catalog,
            model: this.selected.data.properties.source_model,
            code: this.selected.data.properties.source_column
          })
          this.search(true)
          this.selected = null
        } else if (uri === 'cite') {
          Object.assign(this.filters, {
            catalog: this.selected.data.properties.target_catalog,
            model: this.selected.data.properties.target_model,
            code: this.selected.data.properties.target_column
          })
          this.search(true)
          this.selected = null
        }
      } else {
        RouteUtil.forward(this, e, {
          path: '/govern/meta/' + uri,
          query: {
            catalog: this.selected.data.catalog,
            code: this.selected.data.code
          }
        })
      }
    }
  },
  created () {
    this.filters = Object.assign({ minHops: 0, maxHops: 1 }, this.$route.query)
  },
  mounted () {
    const _this = this
    this.er = new ER(this.$refs.relation, {
      onCellClick ({ cell }) {
        _this.selected = cell
        _this.er.highlight([cell])
        return true
      },
      onBlankClick () {
        _this.selected = null
        _this.er.highlight([])
        return true
      }
    })
    this.search(false)
  }
}
</script>

<style lang="less" scoped>
.fs-ui-left {
  margin-top: 25px;
  display: inline-block;
  width: calc(100% - 420px);
  & /deep/ .ant-card-body {
    padding: 0px;
    height: 600px;
  }
  .fs-ui-relation {
    margin: auto;
    width: 100%;
    height: 600px;
  }
}
.fs-ui-right {
  margin-top: 25px;
  float: right;
  width: 400px;
  vertical-align: top;
  & /deep/ .ant-card-body {
    height: 536px;
    overflow: auto;
  }
}
</style>
