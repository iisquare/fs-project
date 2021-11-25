<template>
  <section>
    <a-form-model-item label="表达式">
      <code-editor v-model="value.expression" mode="sql" :hints="hints" :height="100" :lineNumbers="false" />
    </a-form-model-item>
  </section>
</template>

<script>
export default {
  name: 'ColumnCalculate',
  components: {
    CodeEditor: () => import('@/components/Editor/CodeEditor')
  },
  props: {
    value: { type: Object, required: true },
    column: { type: Object, required: true },
    relation: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    hints () {
      const result = []
      if (!this.relation.items) return result
      this.relation.items.forEach((item, tableIndex) => {
        for (const columnIndex in item.columns) {
          const column = item.columns[columnIndex]
          const text = '`' + item.table + '`.`' + column.name + '`'
          result.push({ text: text, displayText: text })
        }
      })
      return result
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
        expression: obj.expression || ''
      }
      return options
    }
  }
}
</script>

<style lang="less" scoped>

</style>
