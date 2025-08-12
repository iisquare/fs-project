<script setup lang="ts">
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue';
import LayoutProperty from '@/components/Layout/LayoutProperty.vue';
import LayoutWidget from '@/components/Layout/LayoutWidget.vue';
import X6Container from '@/designer/X6/X6Container.vue';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import config from '@/designer/KnowledgeGraph/config'
import LayoutToolbar from '@/components/Layout/LayoutToolbar.vue';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import ApiUtil from '@/utils/ApiUtil';
import OntologyApi from '@/api/kg/OntologyApi';
import DesignUtil from '@/utils/DesignUtil';

const route = useRoute()
const router = useRouter()
const flowRef = ref()
const tips: any = ref({})
const diagram:any = ref(Object.assign(config.canvas.options(), { status: '1', notify: {} }))
const activeItem: any = ref({})
const property = computed(() => {
  return DesignUtil.widgetFlowProperty(activeItem.value, config)
})

const options: any = {
  resizing: false,
  rotating: false,
  allowMulti: true,
  allowLoop: true,
}
const handleDragStart = (event: any, widget: any) => {
   flowRef.value.flow.startDrag(event, widget)
}

const loading = ref(false)
const handleSubmit = () => {
  loading.value = true
  const params = Object.assign({}, diagram.value, { content: flowRef.value.flow.toJSON() })
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
  OntologyApi.info(params).then(result => {
    Object.assign(diagram.value, ApiUtil.data(result))
    Object.assign(diagram.value, {
      status: diagram.value.status + '',
    })
    flowRef.value.flow.fromJSON(diagram.value.content?.cells ?? [])
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  OntologyApi.config().then(result => {
    Object.assign(config, ApiUtil.data(result))
  }).catch(() => {})
  handleReload()
})
</script>

<template>
  <LayoutDesigner>
    <template #left>
      <LayoutWidget :widgets="config.widgets" @drag-start="handleDragStart" />
    </template>
    <template #top>
      <LayoutToolbar :toolbars="config.toolbars" :instance="flowRef" />
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
        <el-button @click="router.go(-1)">返回</el-button>
      </el-space>
    </template>
    <template #default>
      <X6Container ref="flowRef" v-model="diagram" :active-item="activeItem" :tips="tips" :options="options" @update:active-item="v => activeItem = v" />
    </template>
    <template #right>
      <LayoutProperty v-model="diagram" :active-item="activeItem" :instance="flowRef" :config="config" :tips="tips" :property="property" />
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
