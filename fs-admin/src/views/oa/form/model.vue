<script setup lang="ts">
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue';
import LayoutProperty from '@/components/Layout/LayoutProperty.vue';
import LayoutWidget from '@/components/Layout/LayoutWidget.vue';
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import config from '@/designer/FlexForm/config';
import LayoutToolbar from '@/components/Layout/LayoutToolbar.vue';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import DesignUtil from '@/utils/DesignUtil';
import ApiUtil from '@/utils/ApiUtil';
import FormFrameApi from '@/api/oa/FormFrameApi';
import FlexFormContainer from '@/designer/FlexForm/FlexFormContainer.vue';

const route = useRoute()
const formRef = ref()
const tips: any = ref({})
const diagram: any = ref({ id: 0, name: '', status: '1', content: Object.assign({ widgets: [] }, config.canvas.options()) })
const activeItem: any = ref({})
const property = computed(() => {
  return DesignUtil.widgetFormProperty(activeItem.value, config)
})

// 后端以字符串存储表单定义，结构为 { widgets: [], options: {} }
const parseContent = (content: any) => {
  if (!content) return {}
  if (typeof content === 'string') {
    try {
      return JSON.parse(content) || {}
    } catch (e) {
      return {}
    }
  }
  return content
}

const collect = () => {
  const content = diagram.value.content || {}
  const options = Object.assign({}, config.canvas.options(), content)
  delete options.widgets
  return JSON.stringify({
    widgets: content.widgets || [],
    options,
  })
}

const loading = ref(false)
const handleSubmit = () => {
  loading.value = true
  const params = {
    id: diagram.value.id || undefined,
    name: diagram.value.name,
    status: diagram.value.status,
    description: diagram.value.description,
    content: collect(),
  }
  FormFrameApi.save(params, { success: true }).then((result: any) => {
    diagram.value.id = result.data.id
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleReload = () => {
  const params = {
    id: route.query.id,
  }
  if (!params.id) return
  loading.value = true
  FormFrameApi.info(params).then(result => {
    const data = ApiUtil.data(result)
    const content = parseContent(data.content)
    const options = content.options || {}
    const frame = Object.assign({}, config.canvas.options(), options, { widgets: content.widgets || [] })
    if (options.labelPosition) {
      frame.labelPosition = options.labelPosition
    } else if (options.align) {
      frame.labelPosition = options.align
    }
    Object.assign(diagram.value, {
      id: data.id,
      name: data.name,
      description: data.description,
      status: data.status + '',
      content: frame,
    })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  FormFrameApi.config().then(result => {
    Object.assign(config, ApiUtil.data(result))
  }).catch(() => {})
  handleReload()
})
</script>

<template>
  <LayoutDesigner splitter>
    <template #left>
      <LayoutWidget :widgets="config.widgets" :native="false" />
    </template>
    <template #top>
      <el-space class="toolbar">
        <LayoutBack to="/oa/form/frame" />
        <el-divider direction="vertical" />
        <LayoutToolbar :toolbars="config.toolbars" :instance="formRef" />
      </el-space>
      <el-space>
        <el-button type="danger" @click="() => formRef.handleClear()" link><LayoutIcon name="Delete" /><span>清空</span></el-button>
        <el-button @click="() => formRef.handlePreview()" link><LayoutIcon name="action.play" /><span>预览</span></el-button>
        <el-divider direction="vertical" />
        <el-button type="primary" @click="handleSubmit" :loading="loading" link>保存</el-button>
      </el-space>
    </template>
    <template #default>
      <FlexFormContainer ref="formRef" v-model="diagram" :active-item="activeItem" :tips="tips" :config="config" @update:active-item="(v: any) => activeItem = v" />
    </template>
    <template #right>
      <LayoutProperty v-model="diagram" :active-item="activeItem" :instance="formRef" :config="config" :tips="tips" :property="property" />
    </template>
    <template #footer>
      <el-space>
        <LayoutIcon name="Opportunity" color="#409eff" />
        <div>{{ tips.text }}</div>
      </el-space>
    </template>
  </LayoutDesigner>
</template>

<style lang="scss" scoped>
</style>
