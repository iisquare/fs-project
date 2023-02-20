<template>
  <section>
    <a-row class="form-row">
      <a-col :md="8" :sm="24">
        <a-form-model-item label="检查表名" prop="checkTable">
          <a-input v-model="value.checkTable" auto-complete="off" placeholder="可通过模型关联"></a-input>
        </a-form-model-item>
      </a-col>
      <a-col :md="8" :sm="24">
        <a-form-model-item label="检查字段" prop="checkColumn">
          <a-input v-model="value.checkColumn" auto-complete="off" placeholder="非字段检查可留空"></a-input>
        </a-form-model-item>
      </a-col>
      <a-col :md="8" :sm="24">
        <a-form-model-item label="聚合方式" prop="checkMetric">
          <a-select v-model="value.checkMetric" placeholder="请选择">
            <a-select-option v-for="item in config.metrics" :key="item.value" :value="item.value" :title="item.label">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-col>
    </a-row>
    <a-row class="form-row">
      <a-col :md="8" :sm="24">
        <a-form-model-item label="判断符号">
          <a-select v-model="value.content.operator" placeholder="请选择">
            <a-select-option v-for="item in config.operators" :key="item.value" :value="item.value" :title="item.label">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-col>
      <a-col :md="16" :sm="24">
        <a-form-model-item label="对比常量">
          <a-input v-model="value.content.value" placeholder="比较的值" />
        </a-form-model-item>
      </a-col>
    </a-row>
    <a-row class="form-row">
      <a-col :md="24" :sm="24">
        <a-form-model-item label="前置条件">
          <a-textarea v-model="value.checkWhere" placeholder="仅检查筛选后的数据记录，先过滤后分组" />
        </a-form-model-item>
      </a-col>
    </a-row>
    <a-row class="form-row">
      <a-col :md="24" :sm="24">
        <a-form-model-item label="分组字段">
          <a-input v-model="value.checkGroup" placeholder="留空时不分组，若不选择聚合方式则逐行进行对比" />
        </a-form-model-item>
      </a-col>
    </a-row>
  </section>
</template>

<script>
export default {
  name: 'ConstCompareRuleProperty',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    defaults () {
      return this.config.ruleDefaults(this.value.type)
    }
  },
  watch: {
    'form.id': {
      handler () {
        this.$emit('input', this.formatted(this.value))
      },
      immediate: true
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        operator: obj.content.operator ?? this.defaults.operator,
        value: obj.content.value ?? this.defaults.value
      }
      return Object.assign(obj, { content: options })
    }
  }
}
</script>

<style lang="less" scoped>

</style>
