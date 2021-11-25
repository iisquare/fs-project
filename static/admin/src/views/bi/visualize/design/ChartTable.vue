<template>
  <section>
    <a-table
      :bordered="false"
      :columns="columns"
      :rowKey="(record, index) => index"
      :dataSource="rows"
      :pagination="false"
    >
      <a-space slot="drill" slot-scope="text, record, index">
        <a-icon type="up-square" @click="drillUp(text, record, index)" v-if="value.length > 0" />
        <a-icon type="down-square" @click="drillDown(text, record, index)" v-if="value.length < axis.xSize - 1" />
      </a-space>
    </a-table>
  </section>
</template>

<script>

export default {
  name: 'ChartTable',
  props: {
    value: { type: Array, required: true }, // 钻取
    config: { type: Object, required: true },
    axis: { type: Object, default: null },
    options: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    columns () {
      const result = []
      if (this.axis.xSize > 1) {
        result.push({ title: '钻取', scopedSlots: { customRender: 'drill' }, width: 80 })
      }
      result.push({ title: this.axis.x.label || 'X-坐标', dataIndex: 'x' })
      for (const columnIndex in this.axis.y) {
        const index = 'y' + columnIndex
        const y = this.axis.y[columnIndex]
        result.push({
          title: y.label || ('Y-坐标-' + columnIndex),
          dataIndex: index,
          sorter: (a, b) => this.sorter(index, a, b)
        })
      }
      return result
    },
    rows () {
      const result = []
      for (const rowIndex in this.axis.x.data) {
        const item = {}
        item.x = this.axis.x.data[rowIndex]
        for (const columnIndex in this.axis.y) {
          item['y' + columnIndex] = this.axis.y[columnIndex].data[rowIndex]
        }
        result.push(item)
      }
      return result
    }
  },
  methods: {
    sorter (index, a, b) {
      return a[index] - b[index]
    },
    drillUp (text, record, index) {
      this.value.pop()
      this.$emit('drill', record, index)
    },
    drillDown (text, record, index) {
      this.value.push({ index, x: record.x })
      this.$emit('drill', record, index)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
