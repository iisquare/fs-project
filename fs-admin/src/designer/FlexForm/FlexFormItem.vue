<script setup lang="ts">
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
  return authority[widget.id]?.viewable
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
    <el-form-item :label="element.label" v-else-if="element.type === 'textarea'">
      <el-input v-model="element.options.value" type="textarea" :placeholder="element.options.placeholder" />
    </el-form-item>
    <el-form-item :label="element.label" v-else-if="element.type === 'password'">
      <el-input v-model="element.options.value" type="password" :placeholder="element.options.placeholder" show-password />
    </el-form-item>
    <el-form-item :label="element.label" v-else-if="element.type === 'number'">
      <el-input-number v-model="element.options.value" :placeholder="element.options.placeholder" :controls="element.options.controls" :controls-position="element.options.controlsPosition">
        <template #prefix>
          <span>{{ element.options.prefix }}</span>
        </template>
        <template #suffix>
          <span>{{ element.options.suffix }}</span>
        </template>
      </el-input-number>
    </el-form-item>
    <el-form-item :label="element.label" v-else-if="element.type === 'radio'">
      <el-radio-group v-model="element.options.value" :class="`fs-${element.options.display}`">
        <el-radio :key="k" :value="v.value" v-for="(v, k) in element.options.items">{{ v.label }}</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item :label="element.label" v-else-if="element.type === 'checkbox'">
      <el-checkbox-group v-model="element.options.value" :class="`fs-${element.options.display}`">
        <el-checkbox :key="k" :value="v.value" v-for="(v, k) in element.options.items" :label="v.label" />
      </el-checkbox-group>
    </el-form-item>
    <el-form-item :label="element.label" v-else-if="element.type === 'select'">
      <el-select
        v-model="element.options.value"
        :multiple="element.options.multiple"
        :clearable="element.options.clearable"
        :filterable="element.options.filterable"
        :allow-create="element.options.allowCreate"
        :reserve-keyword="element.options.reserveKeyword">
        <el-option v-for="(item, index) in element.options.items" :key="index" :label="item.label" :value="item.value" />
      </el-select>
    </el-form-item>
    <el-form-item :label="element.label" v-else-if="element.type === 'switch'">
      <el-switch v-model="element.options.value" inline-prompt :active-text="element.options.active" :inactive-text="element.options.inactive" />
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
    <el-form-item :label="element.label" v-else-if="element.type === 'subform'">{{ element.options.formId }}</el-form-item>
    <el-form-item :label="element.label" v-else>{{ `异常组件 ${element.type} - ${element.id}` }}</el-form-item>
  </div>
</template>

<style lang="scss" scoped>
</style>
