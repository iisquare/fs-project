<template>
  <section>
    <a-layout>
      <a-layout-sider theme="light" width="402">
        <face-select v-model="face" :maxFaceNumber="5" />
      </a-layout-sider>
      <a-layout-content id="result">
        <a-card :bordered="false">
          <div class="table-page-search-wrapper">
            <a-form-model ref="filters" :model="filters" layout="inline">
              <a-row :gutter="12">
                <a-col :span="8">
                  <a-form-model-item label="阈值" prop="threshold">
                    <a-input-number v-model="filters.threshold" :min="0" :max="100"></a-input-number>
                  </a-form-model-item>
                </a-col>
                <a-col :span="8">
                  <a-form-model-item label="TopN" prop="pageSize">
                    <a-input-number v-model="filters.topN" :min="1" :max="100"></a-input-number>
                  </a-form-model-item>
                </a-col>
                <a-col :span="8">
                  <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                  <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
                </a-col>
              </a-row>
            </a-form-model>
            <a-table
              :columns="columns"
              :rowKey="record => record.id"
              :dataSource="rows"
              :loading="loading"
              :bordered="true"
              :pagination="false"
              :scroll="{ x: true, y: 300 }"
            >
              <span slot="image" slot-scope="text, record">
                <img width="112" height="112" :src="record.face" v-if="record.face" />
                <div v-else class="no-image">未设置封面</div>
              </span>
              <span slot="info" slot-scope="text, record">
                ID：{{ record.id }}<br/>
                标识：{{ record.serial }}<br/>
                名称：{{ record.name }}<br/>
                相似度{{ (record.similarity * 100).toFixed(2) + '%' }}<br/>
              </span>
              <span slot="action" slot-scope="text, record">
                <a-button-group>
                  <a-button type="link" size="small" v-permit="'face:user:'" @click="show(text, record)">详情</a-button>
                </a-button-group>
              </span>
            </a-table>
          </div>
        </a-card>
      </a-layout-content>
    </a-layout>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import FaceSelect from '@/views/face/dashboard/select'
import faceService from '@/service/face/face'

export default {
  components: { FaceSelect },
  data () {
    return {
      filters: {
        threshold: 60,
        topN: 10
      },
      face: {},
      columns: [
        { title: '图像', scopedSlots: { customRender: 'image' }, width: 145 },
        { title: '信息', scopedSlots: { customRender: 'info' } },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      rows: [],
      loading: false
    }
  },
  methods: {
    search () {
      if (!this.face.eigenvalue) {
        this.$message.error('请选择左侧面部区域')
        return false
      }
      this.loading = true
      const param = Object.assign({}, this.filters, { eigenvalue: this.face.eigenvalue })
      faceService.search(param).then((result) => {
        if (result.code === 0) {
          this.rows = result.data
        }
        this.loading = false
      })
    },
    show (text, record) {
      const url = this.$router.resolve({
        path: '/face/user/list',
        query: {
          [RouteUtil.filterKey]: RouteUtil.encode({ id: record.id })
        }
      })
      window.open(url.href)
    }
  }
}
</script>

<style lang="less" scoped>
#result {
  padding-left: 15px;
  .no-image {
    width: 112px;
    height: 112px;
    line-height: 112px;
    text-align: center;
    vertical-align: middle;
    font-size: 12px;
    background-color:lightgray;
  }
}
</style>
