<template>
  <section class="fs-subform">
    <a-table
      :columns="columns"
      :rowKey="record => record._id"
      :dataSource="rows"
      :pagination="false"
      :bordered="true"
    >
      <span slot="action" slot-scope="text, record, index">
        <a-button-group>
          <a-button type="link" size="small" @click="show(text, record, index)">查看</a-button>
          <a-button type="link" size="small" @click="edit(text, record, index)" v-if="subformAuthority.changeable">编辑</a-button>
          <a-button type="link" size="small" @click="remove(text, record, index)" v-if="subformAuthority.removable">删除</a-button>
        </a-button-group>
      </span>
    </a-table>
    <a-button type="link" size="small" icon="file-add" @click="add" v-if="subformAuthority.addable">添加</a-button>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form._id" v-model="infoVisible" :destroyOnClose="true" :footer="null">
      <fs-form v-model="form" :frame="subform.options.formInfo" :config="config" :authority="formAuthority.view" />
    </a-modal>
    <!--编辑界面-->
    <a-modal
      :title="'信息' + (form._id ? ('修改 - ' + form._id) : '添加')"
      v-model="formVisible"
      :confirmLoading="formLoading"
      :maskClosable="false"
      :destroyOnClose="true"
      width="818px">
      <fs-form v-model="form" :ref="refForm" :frame="subform.options.formInfo" :config="config" :authority="formAuthority.edit" />
      <template slot="footer">
        <a-button key="back" @click="() => formVisible = false">取消</a-button>
        <a-button key="submit" type="primary" :loading="formLoading" @click="submit(false)">确定</a-button>
        <a-button key="force" type="danger" :loading="formLoading" @click="submit(true)">强制提交</a-button>
      </template>
    </a-modal>
  </section>
</template>

<script>
export default {
  name: 'FsSubform',
  components: { FsForm: () => import('./FsForm') },
  props: {
    value: { type: Array, required: true },
    config: { type: Object, required: true },
    subform: { type: Object, required: true },
    authority: { type: Object, required: true }
  },
  provide () {
    return {
      isFormItemChildren: false // FormItem的isFormItemChildren为true时，wrapperCol不生效
    }
  },
  data () {
    return {
      refForm: this.config.uuidForm(),
      columns: [],
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      formAuthority: { fields: [], view: {}, edit: {} }
    }
  },
  computed: {
    rows () {
      const widgets = [this.config.idField].concat(this.subform.options.formInfo.widgets)
      return this.config.validator.pretty(widgets, this.value)
    },
    subformAuthority () {
      const authority = Object.assign({}, this.config.exhibition.authorityDefaults, this.authority[this.subform.id] || {})
      authority.addable &= authority.editable
      authority.removable &= authority.editable
      authority.changeable &= authority.editable
      return authority
    }
  },
  methods: {
    add () {
      this.form = {}
      this.formVisible = true
    },
    edit (text, record, index) {
      this.form = this.value[index]
      this.formVisible = true
    },
    show (text, record, index) {
      this.form = record
      this.infoVisible = true
    },
    remove (text, record, index) {
      for (const index in this.value) {
        const item = this.value[index]
        if (item._id === record._id) {
          this.value.splice(index, 1)
          return true
        }
      }
      return false
    },
    submit (modeForce) {
      this.$refs[this.refForm].validate(valid => {
        if (!valid) {
          this.$message.warning('数据校验不通过')
          if (!modeForce) return
        }
        this.formVisible = false
        if (this.form._id) {
          for (const index in this.value) {
            const item = this.value[index]
            if (item._id === this.form._id) {
              this.$set(this.value, index, this.form)
              return
            }
          }
        } else {
          this.value.push(Object.assign({ _id: this.config.uuidRecord() }, this.form))
        }
      })
    }
  },
  mounted () {
    const sorted = this.config.exhibition.mergeColumnItem(
      this.config.exhibition.operateFields(this.subform.options.formInfo.widgets, 'viewable', true),
      this.config.exhibition.parseColumnSorted(this.subform.options.column || this.subform.options.formInfo.options.column)
    )
    this.columns = this.config.exhibition.tableColumns(sorted).concat([
      { title: '操作', scopedSlots: { customRender: 'action' } }
    ])
    this.formAuthority.fields = this.config.exhibition.authorityFields(this.subform.options.formInfo.widgets)
    this.formAuthority.view = Object.assign({}, this.authority)
    this.formAuthority.edit = Object.assign({}, this.authority) // 子表单无“可编辑”权限时，不会展示“编辑”按钮，故无需处理子表单内组件的权限值
    for (const key in this.formAuthority.fields) {
      const widget = this.formAuthority.fields[key]
      const authority = this.formAuthority.view[widget.id] || {}
      this.formAuthority.view[widget.id] = { viewable: authority.viewable }
    }
  }
}
</script>

<style lang="less" scoped>
</style>
