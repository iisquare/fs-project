<template>
  <section>
    <a-form-model-item label="日期格式">
      <a-auto-complete
        v-model="value.format"
        optionLabelProp="value"
        :allowClear="true"
        :filterOption="UIUtil.filterOption">
        <template slot="dataSource">
          <a-select-option :key="k" :value="v.value" v-for="(v, k) in formats">{{ v.label }}</a-select-option>
        </template>
      </a-auto-complete>
    </a-form-model-item>
  </section>
</template>

<script>
import UIUtil from '@/utils/ui'

export default {
  name: 'ColumnDate',
  props: {
    value: { type: Object, required: true },
    column: { type: Object, required: true },
    relation: { type: Object, required: true }
  },
  data () {
    return {
      UIUtil,
      formats: [
        { value: 'millisecond', label: '毫秒（millisecond）' },
        { value: 'second', label: '秒（second）' },
        { value: 'yyyy-MM-dd HH:mm:ss', label: 'yyyy-MM-dd HH:mm:ss' },
        { value: 'yyyy-MM-dd', label: 'yyyy-MM-dd' },
        { value: 'HH:mm:ss', label: 'HH:mm:ss' },
        { value: 'yyyyMMdd', label: 'yyyyMMdd' },
        { value: 'yyyyMM', label: 'yyyyMM' },
        { value: 'yyyy', label: 'yyyy' }
      ]
    }
  },
  watch: {
    'column': {
      handler () {
        this.$emit('input', this.formatted(this.value))
      },
      immediate: true
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        format: obj.format || ''
      }
      return options
    }
  }
}
</script>

<style lang="less" scoped>

</style>
