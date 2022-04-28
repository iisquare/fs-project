<template>
  <section>
    <a-card :bordered="false" class="fs-compare-left">
      <a-space class="fs-layout-action">
        <a-button icon="sync" @click="() => selection.revertSelected(this.source, 'code')">反选</a-button>
        <a-button icon="issues-close" @click="diff" :loading="loading">对比</a-button>
        <a-button icon="menu-unfold" @click="extract" :disabled="selection.selectedRows.length === 0">抽取</a-button>
        <a-checkbox v-model="compareName">对比名称</a-checkbox>
        <a-checkbox v-model="compareDescription">对比描述</a-checkbox>
      </a-space>
      <a-table
        class="fs-compare-table"
        :columns="columns"
        :loading="loading"
        :dataSource="source"
        :rowSelection="selection"
        :pagination="false"
        :rowKey="record => record.code"
        :bordered="true"
      >
        <a-descriptions slot="expandedRowRender" slot-scope="record">
          <a-descriptions-item label="字段长度">{{ record.size }}</a-descriptions-item>
          <a-descriptions-item label="小数位数">{{ record.digit }}</a-descriptions-item>
          <a-descriptions-item label="允许为空">{{ record.nullable ? 'Y' : 'N' }}</a-descriptions-item>
          <a-descriptions-item label="字段名称" :span="3">{{ record.name }}</a-descriptions-item>
          <a-descriptions-item label="描述信息" :span="3">{{ record.description }}</a-descriptions-item>
        </a-descriptions>
        <span slot="state" slot-scope="text, record">
          <a-tag color="green" v-if="record.state === 'add'">{{ record.state }}</a-tag>
          <a-tag color="orange" v-else-if="record.state === 'differ'">{{ record.state }}</a-tag>
          <a-tag color="red" v-else-if="record.state === 'delete'">{{ record.state }}</a-tag>
          <a-tag v-else>{{ record.state }}</a-tag>
        </span>
      </a-table>
    </a-card>
    <a-card :bordered="false" class="fs-compare-right">
      <a-space class="fs-layout-action">
        <a-button icon="plus" @click="add(false)">添加</a-button>
        <a-button icon="to-top" @click="add(true)">插入</a-button>
        <a-button icon="edit" @click="edit">编辑</a-button>
        <a-button icon="arrow-up" @click="sortTable.up()">上移</a-button>
        <a-button icon="arrow-down" @click="sortTable.down()">下移</a-button>
        <a-button icon="delete" @click="sortTable.remove()">删除</a-button>
        <a-button icon="history" type="danger" @click="sortTable.reset([])">清空</a-button>
      </a-space>
      <a-table
        class="fs-compare-table"
        :columns="columns"
        :loading="loading"
        :dataSource="sortTable.rows"
        :rowSelection="sortTable.selection"
        :pagination="false"
        :rowKey="(record, index) => index"
        :bordered="true"
      >
        <a-descriptions slot="expandedRowRender" slot-scope="record">
          <a-descriptions-item label="字段长度">{{ record.size }}</a-descriptions-item>
          <a-descriptions-item label="小数位数">{{ record.digit }}</a-descriptions-item>
          <a-descriptions-item label="允许为空">{{ record.nullable ? 'Y' : 'N' }}</a-descriptions-item>
          <a-descriptions-item label="字段名称" :span="3">{{ record.name }}</a-descriptions-item>
          <a-descriptions-item label="描述信息" :span="3">{{ record.description }}</a-descriptions-item>
        </a-descriptions>
        <span slot="state" slot-scope="text, record">
          <a-tag color="green" v-if="record.state === 'add'">{{ record.state }}</a-tag>
          <a-tag color="orange" v-else-if="record.state === 'differ'">{{ record.state }}</a-tag>
          <a-tag color="red" v-else-if="record.state === 'delete'">{{ record.state }}</a-tag>
          <a-tag v-else>{{ record.state }}</a-tag>
        </span>
      </a-table>
    </a-card>
    <!--编辑界面-->
    <a-modal title="属性编辑" v-model="formVisible" :confirmLoading="formLoading" :footer="null">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="字段编码" prop="code">
          <a-input v-model="form.code" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="字段名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="字段类型" prop="type">
          <a-auto-complete v-model="form.type" optionLabelProp="value" :filterOption="UIUtil.filterOption" :allowClear="true">
            <template slot="dataSource">
              <a-select-option :key="k" :value="v" v-for="(v, k) in config.columnTypes">{{ v }}</a-select-option>
            </template>
          </a-auto-complete>
        </a-form-model-item>
        <a-form-model-item label="字段长度" prop="size">
          <a-input-number v-model="form.size" :min="0"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="小数位数" prop="digit">
          <a-input-number v-model="form.digit" :min="0"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="允许为空">
          <a-checkbox v-model="form.nullable">是否允许为空</a-checkbox>
        </a-form-model-item>
        <a-form-model-item label="描述信息">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import UIUtil from '@/utils/ui'
import DataUtil from '@/utils/data'
import RouteUtil from '@/utils/route'
import SortTable from '@/utils/helper/SortTable'

export default {
  name: 'ModelCompareColumn',
  props: {
    value: { type: Array, required: true },
    config: { type: Object, required: true },
    source: { type: Array, required: true }
  },
  data () {
    return {
      UIUtil,
      columns: [
        { title: '字段', dataIndex: 'code' },
        { title: '类型', dataIndex: 'type' },
        { title: '差异', scopedSlots: { customRender: 'state' } }
      ],
      compareName: false,
      compareDescription: false,
      selection: RouteUtil.selection(),
      sortTable: new SortTable([], { columnWidth: 25 }),
      loading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        code: [{ required: true, message: '请输入字段编码', trigger: 'blur' }],
        type: [{ required: true, message: '请输入字段类型', trigger: 'blur' }]
      }
    }
  },
  methods: {
    same (a, b) {
      if (a.type !== b.type) return false
      if (a.size !== b.size) return false
      if (a.digit !== b.digit) return false
      if (a.nullable !== b.nullable) return false
      if (a.nullable !== b.nullable) return false
      if (this.compareName && a.name !== b.name) return false
      if (this.compareDescription && a.description !== b.description) return false
      return true
    },
    diff () {
      if (this.loading) return false
      this.loading = true
      const source = DataUtil.array2map(this.source, 'code')
      const target = DataUtil.array2map(this.sortTable.rows, 'code')
      for (const index in source) {
        const item = source[index]
        const record = target[index]
        if (record) {
          item.state = record.state = this.same(item, record) ? 'same' : 'differ'
        } else {
          item.state = 'add'
        }
      }
      for (const index in target) {
        const item = source[index]
        const record = target[index]
        if (!item) record.state = 'delete'
      }
      this.loading = false
      return true
    },
    extract () {
      const result = []
      for (const index in this.source) {
        const record = this.source[index]
        if (this.selection.selectedRowKeys.indexOf(record.code) !== -1) {
          result.push(Object.assign({}, record))
        }
      }
      this.sortTable.reset(this.sortTable.rows.concat(result))
    },
    add (withInsert) {
      this.form = {
        code: '',
        name: '',
        type: '',
        size: 0,
        digit: 0,
        nullable: false,
        description: ''
      }
      if (withInsert) {
        this.sortTable.insert(this.form)
      } else {
        this.sortTable.add(this.form)
      }
      this.formVisible = true
    },
    edit () {
      const index = this.sortTable.selectedIndex()
      if (index === -1) return false
      this.form = this.sortTable.rows[index]
      this.formVisible = true
    },
    reset () {
      this.sortTable.reset(this.value)
    },
    collect () {
      return this.sortTable.rows
    }
  },
  mounted () {
    this.reset()
    this.diff()
  },
  destroyed () {
    this.$emit('input', this.collect())
  }
}
</script>
<style lang="less" scoped>
.fs-layout-action {
  margin-bottom: 20px;
}
.fs-compare-left {
  width: 50%;
  float: left;
}
.fs-compare-right {
  width: 50%;
  float: right;
}
.fs-compare-table {
  height: calc(100vh - 150px);
  overflow: auto;
}
</style>
