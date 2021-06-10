<template>
  <section class="fs-widget-container">
    <div :class="['fs-form-item']" v-for="item in widgets" :key="item.id">
      <a-form-model-item :label="item.label" :prop="item.options.field" v-if="item.type === 'text'">
        <a-input v-model="value[item.options.field]" :placeholder="item.options.placeholder" />
      </a-form-model-item>
      <a-form-model-item :label="item.label" :prop="item.options.field" v-else-if="item.type === 'textarea'">
        <a-textarea v-model="value[item.options.field]" :placeholder="item.options.placeholder" />
      </a-form-model-item>
      <a-form-model-item :label="item.label" :prop="item.options.field" v-else-if="item.type === 'password'">
        <a-input-password v-model="value[item.options.field]" :placeholder="item.options.placeholder" />
      </a-form-model-item>
      <a-form-model-item :label="item.label" :prop="item.options.field" v-else-if="item.type === 'number'">
        <a-space>
          <a-input-number v-model="value[item.options.field]" :placeholder="item.options.placeholder" />
          <span>{{ item.options.suffix }}</span>
        </a-space>
      </a-form-model-item>
      <a-form-model-item :label="item.label" :prop="item.options.field" v-else-if="item.type === 'radio'">
        <a-radio-group v-model="value[item.options.field]">
          <a-radio :key="k" :value="v.value" v-for="(v, k) in item.options.items" :style="{ display: item.options.display }">{{ v.label }}</a-radio>
        </a-radio-group>
      </a-form-model-item>
      <a-form-model-item :label="item.label" :prop="item.options.field" v-else-if="item.type === 'checkbox'">
        <a-checkbox-group v-model="value[item.options.field]">
          <a-checkbox :key="k" :value="v.value" v-for="(v, k) in item.options.items" :style="{ display: item.options.display }">{{ v.label }}</a-checkbox>
        </a-checkbox-group>
      </a-form-model-item>
      <a-form-model-item :label="item.label" :prop="item.options.field" v-else-if="item.type === 'select'">
        <a-auto-complete v-model="value[item.options.field]" v-if="item.options.mode === 'combobox'" optionLabelProp="value" :placeholder="item.options.placeholder" :allowClear="item.options.allowClear">
          <template slot="dataSource">
            <a-select-option :key="k" :value="v.value" v-for="(v, k) in item.options.items">{{ v.label }}</a-select-option>
          </template>
        </a-auto-complete>
        <a-select v-model="value[item.options.field]" v-else :mode="item.options.mode" :placeholder="item.options.placeholder" :allowClear="item.options.allowClear">
          <a-select-option :key="k" :value="v.value" v-for="(v, k) in item.options.items">{{ v.label }}</a-select-option>
        </a-select>
      </a-form-model-item>
      <a-form-model-item :label="item.label" v-else-if="item.type === 'switch'">
        <a-switch v-model="value[item.options.field]" :checked-children="item.options.txtChecked" :un-checked-children="item.options.txtUnChecked" />
      </a-form-model-item>
      <a-form-model-item :label="item.label" v-else-if="item.type === 'txt'">{{ item.options.txt }}</a-form-model-item>
      <a-form-model-item :label="item.label" v-else-if="item.type === 'html'"><span v-html="item.options.html"></span></a-form-model-item>
      <a-divider :dashed="item.options.dashed" :type="item.options.type" :orientation="item.options.orientation" v-else-if="item.type === 'divider'">{{ item.label }}</a-divider>
      <a-form-model-item :label="item.label" v-else-if="item.type === 'subform'">
        <fs-subform v-model="value[item.options.field]" :config="config" :subform="item" :editable="true" v-if="value[item.options.field]" />
      </a-form-model-item>
      <a-row v-else-if="item.type === 'grid'">
        <a-col :span="c.span" :key="c.id" v-for="c in item.options.items">
          <fs-form-item v-model="value" :config="config" :widgets="c.widgets" />
        </a-col>
        <a-col span="24" v-if="item.options.items.length === 0">
          <a-alert message="注意：当前栅格中未设置任何列" banner />
        </a-col>
      </a-row>
      <span v-else>{{ item.type }} - {{ item.label }} - 异常 - {{ item.id }}</span>
    </div>
  </section>
</template>

<script>
import FsSubform from './FsSubform'

export default {
  name: 'FsFormItem',
  components: { FsSubform },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    widgets: { type: Array, required: true }
  },
  data () {
    return {
    }
  },
  methods: {
  }
}
</script>

<style lang="less" scoped>

</style>
