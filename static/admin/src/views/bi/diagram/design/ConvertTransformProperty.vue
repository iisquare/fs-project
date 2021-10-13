<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="合并方式">
          <a-select v-model="value.options.combine" placeholder="请选择字段合并方式" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in combines" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-divider>字段映射</a-divider>
        <a-table
          :columns="columns"
          :data-source="sortTable.rows"
          size="small"
          :pagination="false"
          :rowSelection="sortTable.selection"
          :rowKey="(record, index) => index">
          <span slot="action" slot-scope="text, record">
            <a-icon type="edit" @click="rowEdit(record)" />
          </span>
        </a-table>
        <a-row class="fs-layout-ctr">
          <a-col span="12">排序：<a-button type="link" @click="sortTable.up()">上移</a-button>/<a-button type="link" @click="sortTable.down()">下移</a-button></a-col>
          <a-col span="12">操作：<a-button type="link" @click="rowAdd">新增</a-button>/<a-button type="link" @click="rowInsert">插入</a-button>/<a-button type="link" @click="sortTable.remove()">删除</a-button></a-col>
        </a-row>
      </a-form-model>
      <a-modal title="字段映射" v-model="formVisible" :footer="null">
        <a-form-model ref="form" :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
          <a-form-model-item label="目标字段" prop="name">
            <a-input v-model="form.target" auto-complete="on"></a-input>
          </a-form-model-item>
          <a-form-model-item label="引用字段" prop="name">
            <a-input v-model="form.source" placeholder="为空时默认与目标字段名称一致"></a-input>
          </a-form-model-item>
          <a-form-model-item label="字段类型" prop="engine">
            <a-select v-model="form.clsType" placeholder="请选择字段类型">
              <a-select-option v-for="(item, key) in dagConfig.clsTypes" :key="key" :value="item.value">{{ item.value }} - {{ item.label }}</a-select-option>
            </a-select>
          </a-form-model-item>
        </a-form-model>
      </a-modal>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import UIUtil from '@/utils/ui'
import dagService from '@/service/bi/dag'
import SortTable from '@/utils/helper/SortTable'

export default {
  name: 'ConvertTransformProperty',
  components: { SliceBasic: () => import('./SliceBasic') },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {
      dagConfig: { clsTypes: [] },
      combines: [{
        value: 'KEEP_SOURCE', label: '保留引用字段'
      }, {
        value: 'REMOVE_SOURCE', label: '删除引用字段'
      }, {
        value: 'ONLY_TARGET', label: '仅保留目标字段'
      }],
      sortTable: new SortTable([], { columnWidth: 25 }),
      columns: [
        { title: '目标', dataIndex: 'target', ellipsis: 'auto' },
        { title: '引用', dataIndex: 'source', ellipsis: 'auto' },
        { title: '类型', dataIndex: 'clsType', ellipsis: 'auto' },
        { title: '', scopedSlots: { customRender: 'action' }, width: 25 }
      ],
      form: {},
      formVisible: false
    }
  },
  computed: {
    defaults () {
      return this.config.widgetDefaults(this.value.type)
    }
  },
  watch: {
    'activeItem.id': {
      handler () {
        this.$emit('input', this.formatted(this.value))
      },
      immediate: true
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        combine: obj.options.combine || this.defaults.combine,
        items: Array.isArray(obj.options.items) ? obj.options.items : obj.defaults.items
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    },
    rowItem () {
      return { target: '', source: '', clsType: undefined }
    },
    rowAdd () {
      this.rowEdit(this.sortTable.add(this.rowItem()))
    },
    rowInsert () {
      this.rowEdit(this.sortTable.insert(this.rowItem()))
    },
    rowEdit (record) {
      this.form = record
      this.formVisible = true
    },
    loadDAGConfig () {
      UIUtil.cache(null, () => dagService.config(), 0).then(result => {
        if (result.code === 0) {
          this.dagConfig = result.data
        }
      })
    }
  },
  mounted () {
    this.loadDAGConfig()
    this.sortTable.reset(this.value.options.items)
  }
}
</script>

<style lang="less" scoped>
.fs-layout-ctr {
  padding: 5px 2px;
  .ant-btn-link {
    padding: 0px 2px;
  }
}
</style>
