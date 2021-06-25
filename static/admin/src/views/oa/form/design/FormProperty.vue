<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="表单属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="主键">{{ value.id }}</a-form-model-item>
        <a-form-model-item label="名称">{{ value.name }}</a-form-model-item>
        <a-form-model-item label="物理表" v-if="value.physicalTable">{{ value.physicalTable }}</a-form-model-item>
        <a-form-model-item label="工作流" v-if="value.bpmId">{{ value.bpmId }}</a-form-model-item>
        <a-form-model-item label="标签对齐">
          <a-radio-group v-model="value.options.align">
            <a-radio-button :value="item.value" v-for="item in aligns" :key="item.value">{{ item.label }}</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="标签类型">
          <a-radio-group v-model="value.options.labelType">
            <a-radio-button :value="item.value" v-for="item in labelTypes" :key="item.value">{{ item.label }}</a-radio-button>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="标签宽度">
          <a-space>
            <a-input-number v-model="value.options.labelWidth" :min="0" />
            <span>px</span>
          </a-space></a-form-model-item>
        <a-form-model-item label="标签栅格">
          <a-slider v-model="value.options.labelSpan" :min="0" :max="24" :marks="gutters" :step="1" />
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
    <a-tab-pane key="table" tab="列表配置">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="分页大小"><a-input-number v-model="value.options.pageSize" :min="0" /></a-form-model-item>
        <a-form-model-item label="列表字段"><a-input v-model="value.options.column" auto-complete="on" placeholder="采用英文逗号分隔" /></a-form-model-item>
        <a-form-model-item label="默认排序"><a-input v-model="value.options.sort" auto-complete="on" placeholder="a,b.asc,c.desc" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'FormProperty',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, default: null }
  },
  data () {
    return {
      gutters: Object.fromEntries([0, 6, 12, 18, 24].map(item => [item, item])),
      labelTypes: [{ value: 'grid', label: '栅格' }, { value: 'px', label: '像素' }],
      aligns: [{ value: 'left', label: '左对齐' }, { value: 'right', label: '右对齐' }, { value: 'top', label: '顶对齐' }]
    }
  },
  computed: {
    defaults () {
      return this.config.form.options()
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        align: obj.options.align || this.defaults.align,
        labelType: obj.options.labelType || this.defaults.labelType,
        labelWidth: Number.isInteger(obj.options.labelWidth) ? obj.options.labelWidth : this.defaults.labelWidth,
        labelSpan: Number.isInteger(obj.options.labelSpan) ? obj.options.labelSpan : this.defaults.labelSpan,
        pageSize: Number.isInteger(obj.options.pageSize) ? obj.options.pageSize : this.defaults.pageSize,
        column: obj.options.column || this.defaults.column,
        sort: obj.options.sort || this.defaults.sort
      }
      const result = Object.assign({}, obj, { options })
      return result
    }
  },
  mounted () {
    this.$emit('input', this.formatted(this.value))
  }
}
</script>

<style lang="less" scoped>

</style>
