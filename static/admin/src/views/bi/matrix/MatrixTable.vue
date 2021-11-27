<template>
  <section>
    <a-table
      :bordered="true"
      :columns="columns"
      :rowKey="(record, index) => index"
      :dataSource="rows"
      :loading="loading"
      :pagination="false"
    />
  </section>
</template>
<script>
export default {
  name: 'MatrixTable',
  props: {
    value: { type: Object, default: null },
    loading: { type: Boolean, default: false }
  },
  data () {
    return {
      glue: '#'
    }
  },
  computed: {
    columns () {
      const result = []
      if (!this.value) return result
      let level = result
      this.value.levels.forEach(item => {
        const column = { title: item.label, children: [] }
        level.push(column)
        level = column.children
      })
      this.value.buckets.forEach(item => {
        level.push({ title: item.label, dataIndex: item.key })
      })
      const matrix = this.matrixColumn(this.value.matrix, this.value.metrics, [])
      return result.concat(matrix)
    },
    rows () {
      if (!this.value) return []
      const result = this.value.x
      const refer = {}
      result.forEach(item => {
        refer[this.unique(this.value.buckets, item)] = item
      })
      this.value.y.forEach(item => {
        this.value.metrics.forEach(metric => {
          const road = item.roads.concat([metric.key]).join(this.glue)
          item.metrics.forEach(line => {
            const row = refer[this.unique(this.value.buckets, line)]
            row[road] = line[metric.key]
          })
        })
      })
      return result
    }
  },
  methods: {
    sorter (index, a, b) {
      return a[index] - b[index]
    },
    matrixColumn (matrix, metrics, roads) {
      const columns = []
      matrix.forEach(item => {
        const road = roads.concat([item.label])
        const column = { title: item.label, children: [] }
        if (item.children.length > 0) {
          column.children = this.matrixColumn(item.children, metrics, road)
        } else {
          metrics.forEach(metric => {
            column.children.push({ title: metric.label, dataIndex: road.concat([metric.key]).join(this.glue) })
          })
        }
        columns.push(column)
      })
      return columns
    },
    unique (buckets, row) {
      const result = []
      buckets.forEach(item => {
        result.push(row[item.key])
      })
      return result.join(this.glue)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
