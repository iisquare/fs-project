<template>
  <section>
    <a-card :bordered="false">
      <a-form-model layout="inline">
        <a-form-model-item label="元数据">
          <a-input-search v-model="filters.keyword" placeholder="关键词" enter-button="检索" @search="search(true, false)" :loading="loading" />
        </a-form-model-item>
        <a-form-model-item label="包前缀">
          <a-input v-model="filters.prefix" placeholder="所属包" :allowClear="true" />
        </a-form-model-item>
        <a-form-model-item label="类型">
          <a-input v-model="filters.mold" placeholder="英文逗号分隔" :allowClear="true" />
        </a-form-model-item>
      </a-form-model>
    </a-card>
    <a-card :bordered="false" style="margin-top: 25px;">
      <a-empty v-if="rows.length === 0" style="padding: 150px;" />
      <a-list item-layout="vertical" size="large" :pagination="pagination" :data-source="rows" v-else>
        <a-list-item slot="renderItem" class="fs-ui-highlight" :key="index" slot-scope="item, index">
          <template slot="extra">
            <a-button type="link" @click="forward('detail', item)">详情</a-button>
            <a-button type="link" @click="forward('blood', item)">血缘</a-button>
          </template>
          <a-list-item-meta>
            <a-form-model layout="inline" slot="title">
              <a-form-model-item label="编码"><div v-html="item.code"></div></a-form-model-item>
              <a-form-model-item label="名称"><div v-html="item.name"></div></a-form-model-item>
              <a-form-model-item label="类型">{{ item.mold }}</a-form-model-item>
            </a-form-model>
            <template slot="description"><div v-html="item.description"></div></template>
          </a-list-item-meta>
          <span>路径：{{ item.id }}</span>
        </a-list-item>
      </a-list>
    </a-card>
  </section>
</template>

<script>
import UIUtil from '@/utils/ui'
import RouteUtil from '@/utils/route'
import metaService from '@/service/govern/meta'

export default ({
  data () {
    return {
      filters: {},
      loading: false,
      rows: [],
      pagination: {}
    }
  },
  methods: {
    search (filter2query, pagination) {
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      filter2query && this.$router.push({ path: this.$route.path, query: this.filters }).catch(() => {})
      this.loading = true
      const param = Object.assign({ highlight: 'code,name,description' }, this.filters)
      metaService.search(param).then(result => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows.map(item => { return Object.assign(item, UIUtil.highlight(item.highlight)) })
        }
        this.loading = false
      })
    },
    forward (uri, item) {
      const ids = item.id.split('/')
      const query = {}
      if (item.mold === 'column') {
        query.column = ids.pop()
        query.code = ids.pop()
      } else if (uri === 'detail' || item.mold !== 'catalog') {
        query.code = ids.pop()
      }
      query.catalog = ids.join('/') + '/'
      this.$router.push({ path: '/govern/meta/' + uri, query })
    }
  },
  created () {
    this.filters = {
      page: 1,
      pageSize: 15,
      prefix: this.$route.query.prefix,
      mold: this.$route.query.mold,
      keyword: this.$route.query.keyword
    }
    const _this = this
    this.pagination = Object.assign({
      onChange (page, pageSize) {
        _this.pagination[RouteUtil.paginationPageKey] = page
        _this.search(true, true)
      },
      onShowSizeChange (page, pageSize) {
        _this.pagination[RouteUtil.paginationPageKey] = 1
        _this.pagination.pageSize = pageSize
        _this.search(true, true)
      }
    }, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
  }
})
</script>
<style lang="less" scoped>
.fs-ui-highlight {
  & /deep/ em {
    color: red;
  }
}
</style>
