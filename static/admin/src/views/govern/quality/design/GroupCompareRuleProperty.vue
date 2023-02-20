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
      <a-col :md="24" :sm="24">
        <a-form-model-item label="前置条件">
          <a-textarea v-model="value.checkWhere" placeholder="仅检查筛选后的数据记录，先过滤后分组" />
        </a-form-model-item>
      </a-col>
    </a-row>
    <a-row class="form-row">
      <a-col :md="24" :sm="24">
        <a-form-model-item label="分组字段">
          <a-input v-model="value.checkGroup" placeholder="分组字段作为记录标识，可配置为联合主键，以英文逗号分隔" />
        </a-form-model-item>
      </a-col>
    </a-row>
    <a-row class="form-row">
      <a-col :md="8" :sm="24">
        <a-form-model-item label="引用表名" prop="referTable">
          <a-input v-model="value.referTable" auto-complete="off" placeholder="可通过模型关联"></a-input>
        </a-form-model-item>
      </a-col>
      <a-col :md="8" :sm="24">
        <a-form-model-item label="引用字段" prop="referColumn">
          <a-input v-model="value.referColumn" auto-complete="off" placeholder="非字段检查可留空"></a-input>
        </a-form-model-item>
      </a-col>
      <a-col :md="8" :sm="24">
        <a-form-model-item label="聚合方式" prop="referMetric">
          <a-select v-model="value.referMetric" placeholder="请选择">
            <a-select-option v-for="item in config.metrics" :key="item.value" :value="item.value" :title="item.label">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-col>
    </a-row>
    <a-row class="form-row">
      <a-col :md="24" :sm="24">
        <a-form-model-item label="引用条件">
          <a-textarea v-model="value.referWhere" placeholder="仅检查筛选后的数据记录，先过滤后分组" />
        </a-form-model-item>
      </a-col>
    </a-row>
    <a-row class="form-row">
      <a-col :md="24" :sm="24">
        <a-form-model-item label="引用分组">
          <a-input v-model="value.referGroup" placeholder="按分组字段的结果，逐行进行对比" />
        </a-form-model-item>
      </a-col>
    </a-row>
  </section>
</template>

<script>
export default {
  name: 'GroupCompareRuleProperty',
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
      }
      return Object.assign(obj, { content: options })
    }
  }
}
</script>

<style lang="less" scoped>

</style>
