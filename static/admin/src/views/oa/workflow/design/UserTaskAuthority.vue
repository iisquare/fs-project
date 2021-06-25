<template>
  <section>
    <a-table
      :columns="columns"
      :data-source="rows"
      size="small"
      :pagination="false"
      :rowKey="record => record.id"
      :expandedRowKeys="expandedRowKeys">
      <span slot="viewable" slot-scope="text, record"><a-checkbox v-model="authority[record.id].viewable" /></span>
      <span slot="editable" slot-scope="text, record"><a-checkbox v-model="authority[record.id].editable" v-if="record.editable" /></span>
      <span slot="action" slot-scope="text, record">
        <a-popover title="">
          <template slot="content">
            <a-form-model :label-col="{ span: 5 }" :wrapper-col="{ span: 15 }" class="fs-authority-action">
              <a-form-model-item label="主键：">{{ record.id }}</a-form-model-item>
              <a-form-model-item label="字段：">{{ record.field }}</a-form-model-item>
              <a-form-model-item label="标签：">{{ record.label }}</a-form-model-item>
              <a-form-model-item label="配置" v-if="record.editable">
                <a-checkbox v-model="authority[record.id].variable">设为流程变量</a-checkbox>
                <a-checkbox v-model="authority[record.id].addable" v-if="record.type === 'subform'">可新增记录</a-checkbox>
                <a-checkbox v-model="authority[record.id].changeable" v-if="record.type === 'subform'">可编辑已有记录</a-checkbox>
                <a-checkbox v-model="authority[record.id].removable" v-if="record.type === 'subform'">可删除已有记录</a-checkbox>
              </a-form-model-item>
            </a-form-model>
          </template>
          <a-icon type="setting" />
        </a-popover>
      </span>
    </a-table>
    <a-row class="fs-authority-ctr">
      <a-col span="12">可见：<a-button type="link" @click="checkedAll('viewable')">全选</a-button>/<a-button type="link" @click="checkedRevert('viewable')">反选</a-button></a-col>
      <a-col span="12">可编辑：<a-button type="link" @click="checkedAll('editable')">全选</a-button>/<a-button type="link" @click="checkedRevert('editable')">反选</a-button></a-col>
    </a-row>
  </section>
</template>

<script>
import config from '../../form/design/config'

export default {
  name: 'UserTaskAuthority',
  props: {
    value: { type: String, required: true },
    bpmn: { type: Object, required: true },
    element: { type: Object, required: true },
    workflow: { type: Object, required: true }
  },
  data () {
    return {
      config,
      rows: [],
      authority: {},
      expandedRowKeys: [],
      columns: [
        { title: '字段', dataIndex: 'label' },
        { title: '可见', scopedSlots: { customRender: 'viewable' } },
        { title: '可编辑', scopedSlots: { customRender: 'editable' } },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ]
    }
  },
  watch: {
    element: {
      handler () {
        this.rows = this.config.exhibition.authorityFields(this.workflow.formInfo?.widgets, this.expandedRowKeys)
        this.authority = this.config.exhibition.authority(this.rows, this.parseValue(this.value))
      },
      immediate: true
    },
    authority: {
      handler (val) {
        this.$emit('input', JSON.stringify(val))
      },
      deep: true
    }
  },
  methods: {
    checkedAll (field) {
      for (const key in this.authority) this.authority[key][field] = true
    },
    checkedRevert (field) {
      for (const key in this.authority) this.authority[key][field] = !this.authority[key][field]
    },
    parseValue (value) {
      try {
        if (!value) return {}
        return JSON.parse(value)
      } catch (err) {
        this.$error({ title: '解析权限配置异常', content: err.message })
        return {}
      }
    }
  }
}
</script>

<style lang="less" scoped>
.fs-authority-action {
  width: 350px;
  .ant-form-item {
    margin-bottom: 0px;
  }
  .ant-checkbox-wrapper {
    display: block;
    margin-left: 0px;
  }
}
.fs-authority-ctr {
  padding: 5px 2px;
  .ant-btn-link {
    padding: 0px 2px;
  }
}
</style>
