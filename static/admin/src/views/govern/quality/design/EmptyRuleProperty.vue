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
          <a-input v-model="value.checkColumn" auto-complete="off" placeholder="单个字段名称"></a-input>
        </a-form-model-item>
      </a-col>
      <a-col :md="8" :sm="24">
        <a-form-model-item label="选项">
          <a-checkbox v-model="value.content.isNull">判断是否为NULL</a-checkbox>
          <a-checkbox v-model="value.content.isBlank">判断是否为空字符串</a-checkbox>
        </a-form-model-item>
      </a-col>
    </a-row>
    <a-row class="form-row">
      <a-col :md="24" :sm="24">
        <a-form-model-item label="前置条件">
          <a-textarea v-model="value.checkWhere" placeholder="仅检查筛选后的数据记录" />
        </a-form-model-item>
      </a-col>
    </a-row>
  </section>
</template>

<script>
export default {
  name: 'EmptyRuleProperty',
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
        isNull: obj.content.isNull ?? this.defaults.isNull,
        isBlank: obj.content.isBlank ?? this.defaults.isBlank
      }
      return Object.assign(obj, { content: options })
    }
  }
}
</script>

<style lang="less" scoped>

</style>
