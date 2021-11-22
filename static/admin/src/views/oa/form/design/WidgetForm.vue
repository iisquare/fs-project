<template>
  <section class="fs-widget-section">
    <draggable
      :value="value"
      @input="value => this.$emit('input', value)"
      class="fs-widget-container"
      group="widgets"
      animation="340"
      @start="handleWidgetStart"
      @add="handleWidgetAdd">
      <div
        :class="['fs-form-item', activeItem && activeItem.id === item.id && 'fs-form-item-active']"
        v-for="item in value"
        :key="item.id"
        @click.stop="triggerItem(item)"
        @contextmenu="ev => handleContextMenu(ev, item)">
        <a-form-model-item :label="item.label" v-if="item.type === 'text'"><a-input :placeholder="item.options.placeholder" /></a-form-model-item>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'textarea'"><a-textarea :placeholder="item.options.placeholder" /></a-form-model-item>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'password'"><a-input-password :placeholder="item.options.placeholder" /></a-form-model-item>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'number'">
          <a-space>
            <a-input-number :placeholder="item.options.placeholder" />
            <span>{{ item.options.suffix }}</span>
          </a-space>
        </a-form-model-item>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'radio'">
          <a-radio-group>
            <a-radio :key="k" :value="v.value" v-for="(v, k) in item.options.items" :style="{ display: item.options.display }">{{ v.label }}</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'checkbox'">
          <a-checkbox-group>
            <a-checkbox :key="k" :value="v.value" v-for="(v, k) in item.options.items" :style="{ display: item.options.display }">{{ v.label }}</a-checkbox>
          </a-checkbox-group>
        </a-form-model-item>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'switch'">
          <a-switch :checked-children="item.options.txtChecked" :un-checked-children="item.options.txtUnChecked" />
        </a-form-model-item>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'select'">
          <a-auto-complete v-if="item.options.mode === 'combobox'" optionLabelProp="value" :filterOption="UIUtil.filterOption" :placeholder="item.options.placeholder" :allowClear="item.options.allowClear">
            <template slot="dataSource">
              <a-select-option :key="k" :value="v.value" v-for="(v, k) in item.options.items">{{ v.label }}</a-select-option>
            </template>
          </a-auto-complete>
          <a-select v-else :mode="item.options.mode" :placeholder="item.options.placeholder" :allowClear="item.options.allowClear">
            <a-select-option :key="k" :value="v.value" v-for="(v, k) in item.options.items">{{ v.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'txt'">{{ item.options.txt }}</a-form-model-item>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'html'"><span v-html="item.options.html"></span></a-form-model-item>
        <a-divider :dashed="item.options.dashed" :type="item.options.type" :orientation="item.options.orientation" v-else-if="item.type === 'divider'">{{ item.label }}</a-divider>
        <a-form-model-item :label="item.label" v-else-if="item.type === 'subform'">{{ item.options.formId }}</a-form-model-item>
        <a-row v-else-if="item.type === 'grid'">
          <a-col :span="c.span" :key="c.id" v-for="c in item.options.items">
            <widget-form
              v-model="c.widgets"
              :config="config"
              :activeItem="activeItem"
              @update:activeItem="val => $emit('update:activeItem', val)"
              :tips="tips"
              @update:tips="val => $emit('update:tips', val)" />
          </a-col>
          <a-col span="24" v-if="item.options.items.length === 0">
            <a-alert message="注意：当前栅格中未设置任何列" banner />
          </a-col>
        </a-row>
        <span v-else>{{ item.type }} - {{ item.label }} - 异常 - {{ item.id }}</span>
      </div>
    </draggable>
  </section>
</template>

<script>
import draggable from 'vuedraggable'
import MenuUtil from '@/utils/menu'
import UIUtil from '@/utils/ui'

export default {
  components: { draggable },
  name: 'WidgetForm',
  props: {
    value: { type: Array, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, default: null },
    tips: { type: String, default: '' }
  },
  data () {
    return {
      UIUtil
    }
  },
  methods: {
    generateItem (widget) {
      return { id: this.config.uuidWidget(), type: widget.type, label: widget.label, icon: widget.icon, options: widget.options() }
    },
    handleWidgetStart (ev) {
      const item = this.value[ev.oldIndex]
      this.triggerItem(item)
    },
    handleWidgetAdd (ev) {
      if (!ev.clone.dataset.id) return true
      const item = this.generateItem(this.config.widgetByType(ev.clone.dataset.id))
      ev.item.remove()
      this.value.splice(ev.newIndex, 0, item)
      this.triggerItem(item)
    },
    triggerItem (item) {
      this.$emit('update:activeItem', item)
      this.$emit('update:tips', '选中组件 - ' + item.id)
    },
    removeItem (item) {
      for (const index in this.value) {
        if (item.id === this.value[index].id) {
          this.value.splice(index, 1)
          this.$emit('update:activeItem', item)
          this.$emit('update:tips', '移除组件 - ' + item.id)
          return true
        }
      }
      return false
    },
    handleContextMenu (ev, item) {
      this.triggerItem(item)
      MenuUtil.context(ev, [
        { title: `${item.type} - ${item.label}`, disabled: true },
        { type: 'divider' },
        { key: 'delete', icon: 'delete', title: '移除组件' },
        { type: 'divider' },
        { title: item.id, disabled: true }
      ], menu => {
        switch (menu.key) {
          case 'delete':
            return this.removeItem(item)
          default:
            return false
        }
      })
    }
  }
}
</script>

<style lang="less" scoped>
.fs-widget-section {
  width: 100%;
  height: 100%;
}
.fs-widget-container {
  width: 100%;
  height: 100%;
  .fs-form-item {
    width: 100%;
    cursor: move;
    border-radius: 3px;
    > .ant-form-item {
      padding: 12px 10px;
      margin: 0px;
    }
  }
  .fs-form-item-active {
    > .ant-form-item, > .ant-divider {
      background-color: #f4f6fc;
    }
    > .ant-row > .ant-col {
      .ant-alert-warning {
        border: 1px dashed #409eff;
      }
      > .fs-widget-section > .fs-widget-container {
        border: 1px dashed #409eff;
      }
    }
  }
  .ant-col {
    padding: 15px 3px;
    > .fs-widget-section > .fs-widget-container {
      min-height: 64px;
      border-radius: 3px;
      border: 1px dashed #ccc;
    }
  }
  .ant-alert-warning {
    border: 1px dashed white;
  }
  .ant-radio-wrapper, .ant-checkbox-wrapper {
    line-height: 32px;
    margin-left: 0px;
    margin-right: 8px;
  }
}
</style>
