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
    fieldKey: { type: String, default: 'id' }, // 唯一标识
    fieldValue: { type: String, default: 'id' }, // 字段值
    fieldLabel: { type: String, default: 'name' } // 字段标签
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
      this.search({ name: content, pageSize: this.pageSize }).then((result) => {
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
