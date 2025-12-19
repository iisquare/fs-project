<script setup lang="ts">
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue';
import LayoutProperty from '@/components/Layout/LayoutProperty.vue';
import LayoutWidget from '@/components/Layout/LayoutWidget.vue';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import config from '@/designer/FlexForm/config';
import LayoutToolbar from '@/components/Layout/LayoutToolbar.vue';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import DesignUtil from '@/utils/DesignUtil';
import ApiUtil from '@/utils/ApiUtil';
import OntologyApi from '@/api/kg/OntologyApi';
import FormFrameApi from '@/api/oa/FormFrameApi';
import FlexFormContainer from '@/designer/FlexForm/FlexFormContainer.vue';

const route = useRoute()
const router = useRouter()
const formRef = ref()
const tips: any = ref({})
const diagram:any = ref({ status: '1', content: Object.assign({ widgets: [] }, config.canvas.options()) })
const activeItem: any = ref({})
const property = computed(() => {
  return DesignUtil.widgetFormProperty(activeItem.value, config)
})

const loading = ref(false)
const handleSubmit = () => {
  loading.value = true
  const params = Object.assign({}, diagram.value)
  OntologyApi.save(params, { success: true }).then((result: any) => {
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
    Object.assign(diagram.value, ApiUtil.data(result))
    Object.assign(diagram.value, {
      status: diagram.value.status + '',
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
  <LayoutDesigner>
    <template #left>
      <LayoutWidget :widgets="config.widgets" :native="false" />
    </template>
    <template #top>
      <LayoutToolbar :toolbars="config.toolbars" :instance="formRef" />
      <el-space>
        <el-button type="danger" @click="() => formRef.handleClear()" link><LayoutIcon name="Delete" /><span>清空</span></el-button>
        <el-button @click="() => formRef.handlePreview()" link><LayoutIcon name="action.play" /><span>预览</span></el-button>
        <el-divider direction="vertical" />
        <el-button type="primary" @click="handleSubmit" :loading="loading" link>保存</el-button>
        <el-button @click="router.go(-1)" link>返回</el-button>
      </el-space>
    </template>
    <template #default>
      <FlexFormContainer ref="formRef" v-model="diagram" :active-item="activeItem" :tips="tips" :config="config" @update:active-item="v => activeItem = v" />
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
