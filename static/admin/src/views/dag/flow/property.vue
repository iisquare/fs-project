<template>
  <section>
    <a-collapse :activeKey="groups.map(item => item.name)" :bordered="false">
      <a-collapse-panel :key="group.name" :header="group.name" v-for="group in groups">
        <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
          <a-form-model-item :label="item.name" :key="item.field" v-for="item in group.properties">
            <a-input-number v-model="value[item.field]" :placeholder="item.placeholder" v-if="item.editor === 'number'" />
            <a-input v-model="value[item.field]" :placeholder="item.placeholder" v-else-if="item.editor === 'text'" />
            <a-textarea v-model="value[item.field]" :placeholder="item.placeholder" v-else-if="item.editor === 'textarea'" />
            <a-checkbox v-model="value[item.field]" v-else-if="item.editor === 'checkbox'" />
            <a-select v-model="value[item.field]" :mode="item.options.mode" v-else-if="item.editor === 'selector'">
              <a-select-option :value="option.value" :key="index" v-for="(option, index) in item.options.items">{{ option.label }}</a-select-option>
            </a-select>
            <span v-else>{{ value[item.field] }}</span>
          </a-form-model-item>
        </a-form-model>
      </a-collapse-panel>
    </a-collapse>
  </section>
</template>

<script>
export default {
  name: 'DagFlowProperty',
  props: {
    value: {
      type: Object,
      required: true
    },
    grid: {
      type: Array,
      required: true
    }
  },
  data () {
    return {
      groups: []
    }
  },
  watch: {
    grid () {
      this.generate()
    }
  },
  methods: {
    generate () {
      const arr = []
      const map = {}
      const obj = {}
      this.grid.forEach(item => {
        let properties = map[item.group]
        if (!properties) {
          arr.push({ name: item.group })
          properties = map[item.group] = []
        }
        properties.push(item)
        if (item.editor === 'divider') return
        let value = this.value[item.field] // 记录值
        if (typeof item.value === 'undefined' || item.value === null) {
          value = item.value // 默认值
        }
        switch (item.editor) {
          case 'number':
            value = Number.parseFloat(value)
            if (Number.isNaN(value) || !Number.isFinite(value)) {
              value = 0
            }
            break
          case 'checkbox':
            if (typeof value === 'string') {
              value = value.toLowerCase() === 'true'
            } else {
              value = !!value
            }
            break
          case 'selector':
            if (['multiple', 'tags'].indexOf(item.options.mode) === -1) break
            if (Array.isArray(value)) break
            try {
              value = JSON.parse(value)
            } catch (e) {}
            if (!Array.isArray(value)) value = []
            break
        }
        obj[item.field] = value
      })
      arr.forEach(item => {
        item.properties = map[item.name]
      })
      this.$emit('input', Object.assign({}, this.value, obj))
      this.groups = arr
    }
  }
}
</script>

<style lang="less" scoped>

</style>
