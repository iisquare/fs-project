<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-table
          :columns="columns"
          :rowKey="record => record.id"
          :dataSource="table.leaves"
          :pagination="false"
          :bordered="true"
          childrenColumnName=""
        >
        </a-table>
      </div>
    </a-card>
  </section>
</template>

<script>
import UIUtil from '@/utils/ui'

export default {
  data () {
    return {
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: 'L-0', dataIndex: 'level_0', customRender: (text, row, index) => UIUtil.tableSpanRender(this.table, index, 0) },
        { title: 'L-1', dataIndex: 'level_1', customRender: (text, row, index) => UIUtil.tableSpanRender(this.table, index, 1) },
        { title: 'L-2', dataIndex: 'level_2', customRender: (text, row, index) => UIUtil.tableSpanRender(this.table, index, 2) },
        { title: 'Name', dataIndex: 'name' }
      ],
      rows: [
        /**
         *    c0      c1      c2
         * r0 a1      b1      c1
         * r1 a1      b1      c2
         * r2 a1      b2      c3
         * r3 a2      b3
         * r4 a3
         */
        { id: 'a1', parentId: '', name: 'L0-a1' },
        { id: 'a2', parentId: '', name: 'L0-a2' },
        { id: 'a3', parentId: '', name: 'L0-a3' },
        { id: 'b1', parentId: 'a1', name: 'L1-b1' },
        { id: 'b2', parentId: 'a1', name: 'L1-b2' },
        { id: 'b3', parentId: 'a2', name: 'L1-b3' },
        { id: 'c1', parentId: 'b1', name: 'L2-c1' },
        { id: 'c2', parentId: 'b1', name: 'L2-c2' },
        { id: 'c3', parentId: 'b2', name: 'L2-c3' }
      ],
      table: {}
    }
  },
  methods: {
  },
  mounted () {
    this.table = UIUtil.tableMatrix(UIUtil.tableTree(this.rows, ''), 3)
    UIUtil.tablePretty(this.table)
  }
}
</script>
