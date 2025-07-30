<script setup lang="ts">
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue';
import LayoutProperty from '@/components/Layout/LayoutProperty.vue';
import LayoutWidget from '@/components/Layout/LayoutWidget.vue';
import X6Container from '@/components/X6/X6Container.vue';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import config from '@/components/TaskFlow/config'
import LayoutToolbar from '@/components/Layout/LayoutToolbar.vue';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import DesignUtil from '@/utils/DesignUtil';
import CronApi from '@/api/server/CronApi';
import ApiUtil from '@/utils/ApiUtil';

const route = useRoute()
const router = useRouter()
const flowRef = ref()
const tips = ref('')
const diagram:any = ref(Object.assign(config.canvas.options(), { status: '1', notify: {} }))
const activeItem: any = ref(diagram.value)
const property = computed(() => {
  return DesignUtil.widgetFlowProperty(activeItem.value, config)
})

const handleDragStart = (event: any, widget: any) => {
   flowRef.value.flow.startDrag(event, widget)
}

watch(activeItem, (cell: any) => {
  flowRef.value.flow.updateCell(cell)
}, { deep: true })

const options: any = {
  onBlankClick () {
    activeItem.value = diagram.value
    tips.value = '选中画布'
    flowRef.value.flow.select()
  },
  onCellClick (data: any) {
    activeItem.value = flowRef.value.flow.cell2meta(data.cell)
    tips.value = `选中 ${activeItem.value.shape} ${activeItem.value.data.name}, ID: ${activeItem.value.id}`
    flowRef.value.flow.select(data.cell)
  },
  onNodeAdded (data: any) {
    activeItem.value = flowRef.value.flow.cell2meta(data.node)
    tips.value = `选中 ${activeItem.value.shape} ${activeItem.value.data.name}, ID: ${activeItem.value.id}`
    flowRef.value.flow.select(data.node)
  },
  onEdgeConnected (data: any) {
    activeItem.value = flowRef.value.flow.cell2meta(data.edge)
    tips.value = `选中 ${activeItem.value.shape} ${activeItem.value.data.name}, ID: ${activeItem.value.id}`
    flowRef.value.flow.select(data.edge)
  }
}

const loading = ref(false)
const handleSubmit = () => {
  loading.value = true
  const params = Object.assign({}, diagram.value, { content: flowRef.value.flow.toJSON() })
  CronApi.flowSave(params, { success: true }).then((result: any) => {
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
  CronApi.flowInfo(params).then(result => {
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
  CronApi.flowConfig().then(result => {
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
      <X6Container ref="flowRef" v-bind="options" />
    </template>
    <template #right>
      <LayoutProperty v-model="activeItem" :instance="flowRef" :property="property" :config="config" :tips="tips" />
    </template>
    <template #footer>
      <el-space>
        <LayoutIcon name="Opportunity" color="#409eff" />
        <div>{{ tips }}</div>
      </el-space>
    </template>
  </LayoutDesigner>
</template>

<style lang="scss" scoped>
</style>
