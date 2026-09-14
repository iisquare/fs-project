<script setup lang="ts">
import FlexSubform from './FlexSubform.vue'

const model: any = defineModel()
const {
  config = {} as any,
  widgets = [] as any,
  authority = {} as any,
} = defineProps<{
  config: { type: null, required: false },
  widgets: any,
  authority: any,
}>()

const pretty = (widget: any) => {
  return config.validator.prettyWidget(widget, model.value[widget.options.field])
}
const viewable = (widget: any) => {
  const item = authority[widget.id]
  // 未配置该字段权限时按只读展示，避免整项空白（如流程节点权限未覆盖到的新增字段）
  return item ? !!item.viewable : true
}
const editable = (widget: any) => {
  return authority[widget.id]?.editable
}
</script>

<template>
  <div :class="['form-item']" v-for="element in widgets" :key="element.id">
    <el-form-item :label="element.label" :prop="element.options.field" v-if="element.type === 'text'">
      <el-input v-model="model[element.options.field]" :placeholder="element.options.placeholder" v-if="editable(element)" />
      <template v-else-if="viewable(element)">{{ pretty(element) }}</template>
    </el-form-item>
    <el-form-item :label="element.label" :prop="element.options.field" v-else-if="element.type === 'textarea'">
      <el-input v-model="model[element.options.field]" type="textarea" :placeholder="element.options.placeholder" v-if="editable(element)" />
      <template v-else-if="viewable(element)">{{ pretty(element) }}</template>
    </el-form-item>
    <el-form-item :label="element.label" :prop="element.options.field" v-else-if="element.type === 'password'">
      <el-input v-model="model[element.options.field]" type="password" show-password :placeholder="element.options.placeholder" v-if="editable(element)" />
      <template v-else-if="viewable(element)">{{ pretty(element) }}</template>
    </el-form-item>
    <el-form-item :label="element.label" :prop="element.options.field" v-else-if="element.type === 'number'">
      <el-space v-if="editable(element) || viewable(element)">
        <el-input-number
          v-model="model[element.options.field]"
          v-if="editable(element)"
          :placeholder="element.options.placeholder"
          :controls="element.options.controls"
          :controls-position="element.options.controlsPosition">
          <template #prefix>
            <span>{{ element.options.prefix }}</span>
          </template>
          <template #suffix>
            <span>{{ element.options.suffix }}</span>
          </template>
        </el-input-number>
        <template v-else>{{ pretty(element) }}</template>
        <span>{{ element.options.suffix }}</span>
      </el-space>
    </el-form-item>
    <el-form-item :label="element.label" :prop="element.options.field" v-else-if="element.type === 'radio'">
      <el-radio-group v-model="model[element.options.field]" :class="`fs-${element.options.display}`" v-if="editable(element)">
        <el-radio :key="k" :value="v.value" v-for="(v, k) in element.options.items">{{ v.label }}</el-radio>
      </el-radio-group>
      <template v-else-if="viewable(element)">{{ pretty(element) }}</template>
    </el-form-item>
    <el-form-item :label="element.label" :prop="element.options.field" v-else-if="element.type === 'checkbox'">
      <el-checkbox-group v-model="model[element.options.field]" :class="`fs-${element.options.display}`" v-if="editable(element)">
        <el-checkbox :key="k" :value="v.value" v-for="(v, k) in element.options.items" :label="v.label" />
      </el-checkbox-group>
      <template v-else-if="viewable(element)">{{ pretty(element) }}</template>
    </el-form-item>
    <el-form-item :label="element.label" :prop="element.options.field" v-else-if="element.type === 'select'">
      <el-select
        v-model="model[element.options.field]"
        v-if="editable(element)"
        :multiple="element.options.mode === 'multiple' || element.options.mode === 'tags'"
        :clearable="element.options.clearable"
        :filterable="element.options.filterable"
        :allow-create="element.options.mode === 'combobox' || element.options.mode === 'tags'"
        :reserve-keyword="element.options.reserveKeyword"
        :placeholder="element.options.placeholder">
        <el-option v-for="(item, index) in element.options.items" :key="index" :label="item.label" :value="item.value" />
      </el-select>
      <template v-else-if="viewable(element)">{{ pretty(element) }}</template>
    </el-form-item>
    <el-form-item :label="element.label" :prop="element.options.field" v-else-if="element.type === 'switch'">
      <el-switch
        v-model="model[element.options.field]"
        inline-prompt
        :active-text="element.options.active"
        :inactive-text="element.options.inactive"
        v-if="editable(element)" />
      <template v-else-if="viewable(element)">{{ pretty(element) }}</template>
    </el-form-item>
    <el-form-item :label="element.label" v-else-if="element.type === 'txt'" :label-width="element.label ? '' : '0px'">
      <div class="fs-txt">{{ element.options.txt }}</div>
    </el-form-item>
    <el-form-item :label="element.label" v-else-if="element.type === 'html'" :label-width="element.label ? '' : '0px'">
      <div class="fs-html" v-html="element.options.html"></div>
    </el-form-item>
    <el-divider v-else-if="element.type === 'divider'" :direction="element.options.direction" :content-position="element.options.position" :border-style="element.options.border">{{ element.label }}</el-divider>
    <el-row v-else-if="element.type === 'grid'"
      style="margin-left: 0; margin-right: 0;"
      :justify="element.options.justify"
      :align="element.options.align"
      :gutter="element.options.gutter">
      <el-col :span="c.span" :key="c.id" v-for="c in element.options.items">
        <FlexFormItem v-model="model" :config="config" :widgets="c.widgets" :authority="authority" />
      </el-col>
      <el-col :span="24" v-if="element.options.items?.length === 0">
        <el-alert title="注意：当前栅格中未设置任何列" type="warning" show-icon :closable="false" />
      </el-col>
    </el-row>
    <el-form-item :label="element.label" v-else-if="element.type === 'subform'">
      <FlexSubform
        v-model="model[element.options.field]"
        :config="config"
        :subform="element"
        :authority="authority"
        v-if="model[element.options.field] && (editable(element) || viewable(element))" />
    </el-form-item>
    <el-form-item :label="element.label" v-else>{{ `异常组件 ${element.type} - ${element.id}` }}</el-form-item>
  </div>
</template>

<style lang="scss" scoped>
@import url('./design.scss');
</style>
