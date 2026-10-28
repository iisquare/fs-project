<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  rule: any
  config: any
}>()

const type = computed(() => props.rule?.type)
const hasMetric = computed(() => ['ConstCompareRule', 'GroupCompareRule'].includes(type.value))
const hasReference = computed(() => ['ReferRule', 'GroupCompareRule'].includes(type.value))

if (!props.rule.content) props.rule.content = {}
</script>

<template>
  <div class="rule-fields">
    <el-row :gutter="18">
      <el-col :span="8">
        <el-form-item label="检查表名">
          <el-input v-model="rule.checkTable" placeholder="可填写模型关联表名" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="检查字段">
          <el-input v-model="rule.checkColumn" placeholder="非字段检查可留空" />
        </el-form-item>
      </el-col>
      <el-col :span="8" v-if="hasMetric">
        <el-form-item label="聚合方式">
          <el-select v-model="rule.checkMetric" placeholder="请选择">
            <el-option v-for="item in config.metrics" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="18" v-if="type === 'EmptyRule'">
      <el-col :span="24">
        <el-form-item label="空值选项">
          <el-checkbox v-model="rule.content.isNull">判断是否为 NULL</el-checkbox>
          <el-checkbox v-model="rule.content.isBlank">判断是否为空字符串</el-checkbox>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="18" v-if="type === 'RangeRule'">
      <el-col :span="12">
        <el-form-item label="最小值">
          <el-input v-model="rule.content.min">
            <template #append><el-checkbox v-model="rule.content.minEnable">启用</el-checkbox></template>
          </el-input>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="最大值">
          <el-input v-model="rule.content.max">
            <template #append><el-checkbox v-model="rule.content.maxEnable">启用</el-checkbox></template>
          </el-input>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="边界">
          <el-checkbox v-model="rule.content.minInclude">包含最小值</el-checkbox>
          <el-checkbox v-model="rule.content.maxInclude">包含最大值</el-checkbox>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="18" v-if="type === 'EnumRule'">
      <el-col :span="24">
        <el-form-item label="字典配置">
          <el-input v-model="rule.content.json" type="textarea" :rows="4" placeholder='JSON 字符串，例如 {"A":"启用","B":"停用"}' />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="18" v-if="['RegularRule', 'FormulaRule'].includes(type)">
      <el-col :span="24">
        <el-form-item :label="type === 'RegularRule' ? '正则表达式' : '公式语句'">
          <el-input
            v-model="rule.content.expression"
            type="textarea"
            :rows="3"
            :placeholder="type === 'RegularRule' ? '正则表达式' : 'SQL 判断表达式'"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="18" v-if="['ConstCompareRule', 'ReferRule', 'GroupCompareRule'].includes(type)">
      <el-col :span="24">
        <el-form-item label="前置条件">
          <el-input v-model="rule.checkWhere" type="textarea" :rows="2" placeholder="仅检查筛选后的数据记录" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="18" v-if="['ConstCompareRule', 'GroupCompareRule'].includes(type)">
      <el-col :span="24">
        <el-form-item label="分组字段">
          <el-input v-model="rule.checkGroup" placeholder="多个字段以英文逗号分隔；留空时不分组" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="18" v-if="type === 'ConstCompareRule'">
      <el-col :span="8">
        <el-form-item label="判断符号">
          <el-select v-model="rule.content.operator">
            <el-option v-for="item in config.operators" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="16">
        <el-form-item label="对比常量">
          <el-input v-model="rule.content.value" placeholder="比较的值" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="18" v-if="hasReference">
      <el-col :span="8">
        <el-form-item label="引用表名">
          <el-input v-model="rule.referTable" placeholder="关联表名" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="引用字段">
          <el-input v-model="rule.referColumn" placeholder="非字段检查可留空" />
        </el-form-item>
      </el-col>
      <el-col :span="8" v-if="type === 'GroupCompareRule'">
        <el-form-item label="引用聚合">
          <el-select v-model="rule.referMetric" placeholder="请选择">
            <el-option v-for="item in config.metrics" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="引用条件">
          <el-input v-model="rule.referWhere" type="textarea" :rows="2" placeholder="仅检查筛选后的数据记录" />
        </el-form-item>
      </el-col>
      <el-col :span="24" v-if="type === 'GroupCompareRule'">
        <el-form-item label="引用分组">
          <el-input v-model="rule.referGroup" placeholder="按分组字段逐行对比" />
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.rule-fields {
  padding: 16px 16px 0;
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-ultra-light);
}
</style>
