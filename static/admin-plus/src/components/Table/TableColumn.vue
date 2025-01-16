<script lang="tsx">
import { ElTableColumn } from 'element-plus'
import { defineComponent } from 'vue'

/**
 * 扩展参数
 * hide: 隐藏展示列，默认为false
 * slot: 对应#default插槽
 */
const render = (columns: Array<any>, keyPrefix: string, context: any) => {
  return columns && columns.map((column, index) => (
    !column.hide && <ElTableColumn
      label={column.label}
      prop={column.prop}
      width={column.width}
      fixed={column.fixed}
      key={keyPrefix + '-' + index}
      v-slots={ column.slot && {
        default: context.slots[column.slot]
      }}
    >
      { render(column.children, keyPrefix + '-' + index, context) }
    </ElTableColumn>
  ))
}

export default defineComponent({
  props: {
    columns: Array<any>,
    keyPrefix: {
      type: String,
      default: 'fs',
      required: false,
    },
  },
  setup(props, context) {
    return () => render(props.columns ?? [], props.keyPrefix, context)
  }
})
</script>

<style lang="scss" scoped>
</style>
