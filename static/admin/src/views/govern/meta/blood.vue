<template>
  <section>
    <a-card :bordered="false">
      <a-form-model ref="filters" :model="filters" layout="inline">
        <a-form-model-item label="所属包">
          <a-input v-model="filters.catalog" placeholder="关键词" :allowClear="true" />
        </a-form-model-item>
        <a-form-model-item label="模型编码">
          <a-input v-model="filters.code" placeholder="所属包" :allowClear="true" />
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
        <a-space v-if="selected === null">
          <a-button type="link" @click.native="$router.go(-1)">返回</a-button>
        </a-space>
        <a-space v-else-if="selected && selected.source">
          <a-button type="link" @click="e => forward('detail', e)">详情</a-button>
        </a-space>
        <a-space v-else>
          <a-button type="link" @click="e => forward('detail', e)">详情</a-button>
          <a-button type="link" @click="e => forward('blood', e)">血缘</a-button>
        </a-space>
      </template>
      <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" v-if="selected === null">
        <a-form-model-item label="节点数量">{{ Object.values(blood.nodes).length }}</a-form-model-item>
        <a-form-model-item label="关系数量">{{ Object.values(blood.relations).length }}</a-form-model-item>
      </a-form-model>
      <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" v-else-if="selected.source">
        <a-form-model-item label="来源包">{{ selected.data.properties.source_catalog }}</a-form-model-item>
        <a-form-model-item label="来源模型">{{ selected.data.properties.source_model }}</a-form-model-item>
        <a-form-model-item label="来源字段">{{ selected.data.properties.source_column }}</a-form-model-item>
        <a-form-model-item label="关联关系">{{ selected.data.properties.relation }}</a-form-model-item>
        <a-form-model-item label="目标包">{{ selected.data.properties.target_catalog }}</a-form-model-item>
        <a-form-model-item label="目标模型">{{ selected.data.properties.target_model }}</a-form-model-item>
        <a-form-model-item label="目标字段">{{ selected.data.properties.target_column }}</a-form-model-item>
      </a-form-model>
      <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" v-else>
        <a-form-model-item label="路径">{{ selected.data.properties.id }}</a-form-model-item>
        <a-form-model-item label="所属包">{{ selected.data.properties.catalog }}</a-form-model-item>
        <a-form-model-item label="模型编码">{{ selected.data.properties.code }}</a-form-model-item>
        <a-form-model-item label="模型名称">{{ selected.data.properties.name }}</a-form-model-item>
        <a-form-model-item label="模型类型">{{ selected.data.properties.type }}</a-form-model-item>
      </a-form-model>
    </a-card>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import metaService from '@/service/govern/meta'
import Relationship from '@/utils/helper/Relationship'

export default {
  data () {
    return {
      filters: {},
      loading: false,
      relationship: null,
      selected: null,
      blood: { nodes: {}, relations: {} }
    }
  },
  methods: {
    search (filter2query) {
      this.loading = true
      filter2query && this.$router.push({ path: this.$route.path, query: this.filters }).catch(() => {})
      metaService.blood(this.filters).then(result => {
        if (result.code === 0) {
          this.blood = result.data
          this.renderBlood()
        }
        this.loading = false
      })
    },
    renderBlood () {
      const nodes = Object.values(this.blood.nodes).map(item => {
        return {
          id: item.identity,
          name: item.properties.name,
          data: item
        }
      })
      const links = Object.values(this.blood.relations).map(item => {
        return {
          id: item.identity,
          source: this.blood.nodes[item.start].identity,
          target: this.blood.nodes[item.end].identity,
          label: item.properties.relation,
          data: item
        }
      })
      this.relationship.reset(nodes, links)
    },
    forward (uri, e) {
      if (!this.selected) return
      if (this.selected.source) { // 关系
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
        }
      } else {
        if (uri === 'blood') {
          this.filters.catalog = this.selected.data.properties.catalog
          this.filters.code = this.selected.data.properties.code
          this.search(true)
        } else {
          RouteUtil.forward(this, e, {
            path: '/govern/meta/' + uri,
            query: {
              catalog: this.selected.data.properties.catalog,
              code: this.selected.data.properties.code
            }
          })
        }
      }
    }
  },
  created () {
    this.filters = Object.assign({ minHops: 0, maxHops: 1 }, this.$route.query)
  },
  mounted () {
    this.relationship = new Relationship(this.$refs.relation, {
      onCanvasClick: e => { this.selected = null },
      onLinkClick: (e, d) => { this.selected = d },
      onNodeClick: (e, d) => { this.selected = d }
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
& /deep/ .fs-ui-relation {
  .fs-edge-line {
    stroke: #595D68;
    stroke-width: 1;
  }
  .fs-edge-label {
    cursor: pointer;
  }
  .fs-edge-selected {
    stroke: rgb(192, 57, 57);
  }
  .fs-node-circle {
    fill: rgb(205 153 76);
    stroke: rgb(205 153 76);
    cursor: pointer;
  }
  .fs-node-text {
    pointer-events: none;
    fill: #ffffff;
  }
  .fs-node-selected {
    fill: rgb(192, 57, 57);
    stroke: rgb(192, 57, 57);
  }
}
</style>
