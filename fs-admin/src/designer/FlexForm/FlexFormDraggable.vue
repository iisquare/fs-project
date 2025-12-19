<script setup lang="ts">
import DesignUtil from '@/utils/DesignUtil'
import MenuUtil from '@/utils/MenuUtil'
import { computed, inject, onMounted } from 'vue'
import draggable from 'vuedraggable'

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const emit = defineEmits(['update:activeItem'])
const { // 子组件中，无法整个替换defineModel的value值
  config = {} as any,
  activeItem = {} as any,
} = defineProps<{
  config: { type: null, required: false },
  activeItem: { type: null, required: true },
}>()

const widgetMap: any = computed(() => DesignUtil.widgetMap(config.widgets))

const generateItem = (widget: any) => {
  return { id: config.uuidWidget(), type: widget.type, label: widget.label, icon: widget.icon, options: widget.options() }
}
const removeItem: any = inject('removeItem')

const handleDragAdd = (event: any) => {
  if (!event.clone.dataset.id) return true
  const item = generateItem(widgetMap.value[event.clone.dataset.id])
  model.value.splice(event.newIndex, 1, item)
  handleClick(item)
}

const handleClick = (element: any) => {
  emit('update:activeItem', element)
  tips.value.text = `选中组件 - ${element.id}`
}

const handleContextMenu = (event: any, element: any) => {
  MenuUtil.context(event, [
    { title: `${element.type} - ${element.label}`, disabled: true },
    { type: 'divider' },
    { key: 'delete', icon: 'Delete', title: '移除组件' },
    { type: 'divider' },
    { title: element.id, disabled: true }
  ], (menu: any) => {
    switch (menu.key) {
      case 'delete':
        return removeItem(element)
      default:
        return false
    }
  })
}

onMounted(() => {
})

</script>

<template>
  <draggable
    :list="model"
    item-key="id"
    group="widgets"
    animation="340"
    class="container"
    @add="handleDragAdd">
    <template #item="{ element }">
      <div :class="['form-item', element.id === activeItem.id && 'active']" @click.stop="handleClick(element)" @contextmenu="ev => handleContextMenu(ev, element)">
        <el-form-item :label="element.label" v-if="element.type === 'text'">
          <el-input v-model="element.options.value" :placeholder="element.options.placeholder" />
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
            <FlexFormDraggable v-model="c.widgets" :active-item="activeItem" :config="config" :tips="tips" @update:active-item="v => emit('update:activeItem', v)" />
          </el-col>
          <el-col :span="24" v-if="element.options.items?.length === 0">
            <el-alert title="注意：当前栅格中未设置任何列" type="warning" show-icon :closable="false" />
          </el-col>
        </el-row>
        <el-form-item :label="element.label" v-else-if="element.type === 'subform'">{{ element.options.formId }}</el-form-item>
        <el-form-item :label="element.label" v-else>{{ `异常组件 ${element.type} - ${element.id}` }}</el-form-item>
      </div>
    </template>
  </draggable>
</template>

<style lang="scss" scoped>
.container {
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 136px);
}
.sortable-ghost {
  border-top: solid 3px #5959df;
}
:deep(li.sortable-ghost) {
  list-style: none;
  width: 100%;
  height: 60px;
  background-color: #f6f7ff;
  border-top: solid 3px #5959df;
  .el-icon {
    margin: 0px 6px;
    font-size: 14px;
  }
}
.el-col {
  padding: 3px;
  box-sizing: border-box;
  > .container {
    min-height: 64px;
    border-radius: 3px;
    border: 1px dashed #ccc;
  }
  > .el-alert {
    border: 1px dashed white;
  }
}
.form-item {
  width: 100%;
  cursor: move;
  border-radius: 3px;
  > .el-form-item {
    padding: 12px 10px;
    margin: 0px;
  }
}
.form-item.active {
  > .el-form-item {
    background-color: #f4f6fc;
  }
  > .el-divider {
    background-color: var(--el-color-primary);
  }
  > .el-row > .el-col {
    > .el-alert {
      border: 1px dashed #409eff;
    }
    > .container {
      border: 1px dashed #409eff;
    }
  }
}
</style>
