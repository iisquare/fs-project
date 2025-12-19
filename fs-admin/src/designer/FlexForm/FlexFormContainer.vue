<script setup lang="ts">
import { computed, onMounted, provide, ref, watch } from 'vue'
import FlexFormDraggable from './FlexFormDraggable.vue';
import ElementUtil from '@/utils/ElementUtil';
import FlexForm from './FlexForm.vue';
import { ElMessage } from 'element-plus';

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

const width = ref('770px')
const handleClick = () => {
  emit('update:activeItem', {})
  tips.value.text = '选中表单'
}

const formLayout = computed(() => {
  return config.exhibition.formLayout(model.value.content)
})

const removeItem = (item: any) => {
  for (const index in model.value.content.widgets) {
    const widget = model.value.content.widgets[index]
    if (item.id === widget.id) {
      model.value.content.widgets.splice(index, 1)
      tips.value.text = `移除组件 - ${item.id}`
      break
    }
  }
  handleClick()
  return this
}

const handleClear = () => {
  ElementUtil.confirm('确认清除所有表单内容吗？').then(() => {
    model.value.content.widgets = []
    handleClick()
  }).catch(() => {})
}

const formRef = ref()
const formPreview = ref({})
const formAuthority = ref(config.exhibition.authorityEmpty())
const dialogVisible = ref(false)
const handlePreview = () => {
  formPreview.value = {}
  formAuthority.value = config.exhibition.authorityGrantAll(model.value.content.widgets)
  dialogVisible.value = true
}
const handleSubmit = () => {
  formRef.value.validate((valid: boolean) => {
    if (valid) {
      ElMessage.success('表单项校验通过')
    } else {
      ElMessage.warning('表单项校验未通过')
    }
  })
}

onMounted(() => {
})

provide('removeItem', removeItem)
defineExpose({ width, removeItem, handleClick, handleClear, handlePreview })
</script>

<template>
  <div class="viewport" @click="handleClick">
    <el-form v-bind="formLayout">
      <FlexFormDraggable v-model="model.content.widgets" :active-item="activeItem" :config="config" :tips="tips" @update:active-item="v => emit('update:activeItem', v)" />
      <el-empty description="从左侧拖入组件进行表单设计" class="empty" v-if="model.content.widgets.length === 0" />
    </el-form>
  </div>
  <el-dialog v-model="dialogVisible" title="表单预览" :width="width" :close-on-click-modal="false" :destroy-on-close="true">
    <FlexForm ref="formRef" v-model="formPreview" :config="config" :frame="model" :authority="formAuthority.edit" />
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleSubmit">校验</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
@import url('./design.scss');
.viewport {
  width: v-bind(width);
  height: 100%;
  box-sizing: border-box;
  overflow-x: auto;
  background: white;
  margin: 0 auto;
  .el-form {
    width: 100%;
    min-height: 100%;
  }
}
.empty {
  position: absolute;
  top: calc(50% - 150px);
  left: calc(50% - 125px);
  width: 250px;
}
</style>
