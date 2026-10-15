<script setup lang="ts">
import { computed } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

/** 依据节点类型取参数编辑字段 */
const fields = computed(() => {
  const type = model.value?.data?.type
  return (props.config?.fields ?? {})[type] ?? []
})

const options = computed(() => {
  if (!model.value.data.options || typeof model.value.data.options !== 'object') {
    model.value.data.options = {}
  }
  return model.value.data.options
})

const handleDelete = () => {
  props.instance.flow.remove(model.value)
}

const render = (value: any) => {
  if (undefined === value || null === value) return ''
  if ('object' === typeof value) return JSON.stringify(value, null, 2)
  return String(value)
}

const parse = (key: string, text: string) => {
  try {
    options.value[key] = text.trim() ? JSON.parse(text) : ''
  } catch (e) {
    options.value[key] = text
  }
}

const setRoot = (text: string) => {
  try {
    const parsed = text.trim() ? JSON.parse(text) : {}
    model.value.data.options = (parsed && 'object' === typeof parsed && !Array.isArray(parsed)) ? parsed : {}
  } catch (e) {
    // 非法JSON暂不写回，保留上次有效配置
  }
}
</script>

<template>
  <el-tabs model-value="property" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model">
        <el-form-item label="" class="title">
          <span>基础信息</span>
          <el-popconfirm title="确认删除该节点？" width="180" @confirm="handleDelete">
            <template #reference>
              <LayoutIcon name="Delete" class="delete" />
            </template>
          </el-popconfirm>
        </el-form-item>
        <el-form-item label="类型">{{ model.data.type }}</el-form-item>
        <el-form-item label="名称">
          <el-input v-model="model.data.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.data.description" />
        </el-form-item>
        <el-form-item label="并行度">
          <el-input-number v-model="model.data.parallelism" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="参数前缀">
          <el-input v-model="model.data.kvConfigPrefix" placeholder="用于引用上游配置项" />
        </el-form-item>
        <template v-if="fields.length">
          <el-form-item label="" class="title">参数配置</el-form-item>
          <el-form-item :label="field.label" v-for="field in fields" :key="field.key">
            <el-switch v-if="field.component === 'switch'" v-model="options[field.key]" />
            <el-input-number v-else-if="field.component === 'number'" v-model="options[field.key]" :controls="false" />
            <el-select v-else-if="field.component === 'select'" v-model="options[field.key]" clearable :placeholder="field.placeholder">
              <el-option v-for="item in field.options" :key="item" :value="item" :label="item" />
            </el-select>
            <el-select v-else-if="field.component === 'timezone'" v-model="options[field.key]" clearable>
              <el-option v-for="item in (config.timezones || [])" :key="item" :value="item" :label="item" />
            </el-select>
            <el-select v-else-if="field.component === 'locale'" v-model="options[field.key]" clearable>
              <el-option v-for="item in (config.locales || [])" :key="item" :value="item" :label="item" />
            </el-select>
            <el-input
              v-else-if="field.component === 'textarea'"
              type="textarea"
              :rows="4"
              v-model="options[field.key]"
              :placeholder="field.placeholder"
            />
            <el-input
              v-else-if="field.component === 'json'"
              type="textarea"
              :rows="5"
              :model-value="render(options[field.key])"
              :placeholder="field.placeholder"
              @update:model-value="(value: string) => parse(field.key, value)"
            />
            <el-input
              v-else-if="field.component === 'sql'"
              type="textarea"
              :rows="8"
              v-model="options[field.key]"
              :placeholder="field.placeholder"
            />
            <el-input v-else v-model="options[field.key]" :placeholder="field.placeholder" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="" class="title">参数配置</el-form-item>
          <el-form-item label="参数JSON">
            <el-input
              type="textarea"
              :rows="8"
              :model-value="render(model.data.options)"
              @update:model-value="setRoot"
            />
          </el-form-item>
        </template>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
