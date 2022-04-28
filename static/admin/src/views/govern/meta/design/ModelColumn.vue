<template>
  <section>
    <a-card :bordered="false">
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
        :columns="columns"
        :loading="loading"
        :dataSource="sortTable.rows"
        :rowSelection="sortTable.selection"
        :pagination="false"
        :rowKey="(record, index) => index"
        :bordered="true"
      >
        <a-descriptions slot="expandedRowRender" slot-scope="record">
          <a-descriptions-item label="描述信息" :span="3">{{ record.description }}</a-descriptions-item>
        </a-descriptions>
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
import SortTable from '@/utils/helper/SortTable'

export default {
  name: 'ModelColumn',
  props: {
    value: { type: Array, required: true },
    config: { type: Object, required: true }
  },
  data () {
    return {
      UIUtil,
      columns: [
        { title: '字段编码', dataIndex: 'code' },
        { title: '字段名称', dataIndex: 'name' },
        { title: '字段类型', dataIndex: 'type' },
        { title: '字段长度', dataIndex: 'size' },
        { title: '小数位数', dataIndex: 'digit' },
        { title: '允许为空', dataIndex: 'nullable', customRender: v => v ? 'Y' : 'N' }
      ],
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
</style>
