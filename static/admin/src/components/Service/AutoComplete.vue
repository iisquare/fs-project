<template>
  <a-select
    show-search
    :value="label"
    :placeholder="placeholder"
    :show-arrow="false"
    :allowClear="allowClear"
    @search="handleSearch"
    @change="handleChange"
  >
    <a-select-option v-for="item in rows" :key="item[fieldKey]" :record="item">{{ item[fieldLabel] }}</a-select-option>
  </a-select>
</template>

<script>
export default {
  name: 'ServiceAutoComplete',
  props: {
    search: { type: Function, required: true },
    value: { type: [Number, String], default: undefined },
    label: { type: String, default: undefined },
    placeholder: { type: String, default: '' },
    allowClear: { type: Boolean, default: true },
    pageSize: { type: Number, default: 5 },
    exceptIds: { type: [String, Array], default: undefined }, // 排除记录
    fieldKey: { type: String, default: 'id' }, // 唯一标识
    fieldValue: { type: String, default: 'id' }, // 字段值
    fieldLabel: { type: String, default: 'name' }, // 字段标签
    fieldExcept: { type: String, default: 'exceptIds' } // 排除记录字段
  },
  data () {
    return {
      loading: false,
      rows: []
    }
  },
  methods: {
    handleSearch (content) {
      this.loading = true
      let exceptIds = ''
      if (this.exceptIds) {
        exceptIds = Array.isArray(this.exceptIds) ? this.exceptIds.join(',') : this.exceptIds
      }
      this.search({ name: content, exceptIds, pageSize: this.pageSize }).then((result) => {
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    handleChange (value, option) {
      this.$emit('input', option ? option.data.attrs.record[this.fieldValue] : undefined)
      this.$emit('update:label', option ? option.data.attrs.record[this.fieldLabel] : undefined)
    }
  },
  mounted () {
    this.handleSearch(this.label)
  }
}
</script>

<style lang="less" scoped>

</style>
