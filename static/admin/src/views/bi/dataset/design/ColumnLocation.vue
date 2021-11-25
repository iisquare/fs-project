<template>
  <section>
    <a-form-model-item label="地理范围">
      <a-select v-model="value.dimension" placeholder="请选择范围格式">
        <a-select-option v-for="item in dimensions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
      </a-select>
    </a-form-model-item>
  </section>
</template>

<script>
import UIUtil from '@/utils/ui'

export default {
  name: 'ColumnLocation',
  props: {
    value: { type: Object, required: true },
    column: { type: Object, required: true },
    relation: { type: Object, required: true }
  },
  data () {
    return {
      UIUtil,
      dimensions: [
        { value: 'province', label: '省' },
        { value: 'city', label: '市' },
        { value: 'county', label: '区县' }
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
        dimension: obj.dimension || ''
      }
      return options
    }
  }
}
</script>

<style lang="less" scoped>

</style>
