<script lang="tsx">
/**
 * 动态表格列组件 - 根据列配置数组递归渲染 el-table-column，支持多级表头、隐藏列、自定义 slot。
 *
 * @prop {ColumnConfig[]} columns    - 列配置数组
 * @prop {String}         keyPrefix  - key 前缀，用于递归区分，默认 'fs'
 *
 * 列配置结构 (ColumnConfig):
 *   { label: string, prop?: string, width?: string|number, fixed?: boolean|string, align?: string,
 *     headerAlign?: string, formatter?: Function, hide?: boolean, slot?: string, children?: ColumnConfig[] }
 *   hide   - 隐藏该列，默认 false
 *   slot   - 对应父组件中的插槽名，用于自定义列内容渲染
 *
 * @example
 * <el-table :data="list">
 *   <table-column :columns="[
 *     { label: '名称', prop: 'name' },
 *     { label: '操作', slot: 'actions' },
 *   ]" />
 * </el-table>
 */
import { ElTableColumn } from 'element-plus'
import { defineComponent } from 'vue'

const render = (columns: Array<any>, keyPrefix: string, context: any) => {
  return columns && columns.map((column, index) => (
    !column.hide && <ElTableColumn
      label={column.label}
      prop={column.prop}
      width={column.width}
      fixed={column.fixed}
      align={column.align}
      headerAlign={column.headerAlign}
      formatter={column.formatter}
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
