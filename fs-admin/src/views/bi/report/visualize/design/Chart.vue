<script setup lang="ts">
import { ref } from 'vue';
import ChartTable from './ChartTable.vue';
import VisualizeApi from '@/api/bi/VisualizeApi';
import ApiUtil from '@/utils/ApiUtil';

defineOptions({ name: 'Chart' })

const props = defineProps({
  value: { type: Number, default: 0 },
  type: { type: String, required: true },
  config: { type: Object, required: true },
  options: { type: Object, default: () => ({}) },
})

const axis: any = ref(null)
const levels: any = ref([])
const loading = ref(false)
const lastPreview: any = ref(null)

const reload = () => {
  if (!props.value) return false
  if (loading.value) return false
  loading.value = true
  VisualizeApi.search({ id: props.value, levels: levels.value }).then((result: any) => {
    if (ApiUtil.succeed(result)) {
      axis.value = result.data
      levels.value = result.data.levels || []
    }
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const preview = (datasetId: any, previewOption: any, drillLevels: any = []) => {
  if (loading.value) return false
  loading.value = true
  lastPreview.value = { datasetId, preview: previewOption, levels: drillLevels }
  return VisualizeApi.search(lastPreview.value).then((result: any) => {
    if (ApiUtil.succeed(result)) {
      axis.value = result.data
      levels.value = result.data.levels || []
    }
    return result
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const drill = () => {
  if (lastPreview.value) {
    return preview(lastPreview.value.datasetId, lastPreview.value.preview, levels.value)
  }
  return reload()
}

defineExpose({ reload, preview, drill, axis })
</script>

<template>
  <div class="fs-chart">
    <div class="fs-chart__state" v-if="loading"><el-skeleton :rows="4" animated /></div>
    <div class="fs-chart__state" v-else-if="!axis"><el-empty description="暂无数据" /></div>
    <div class="fs-chart__body" v-else-if="type === 'Table'">
      <ChartTable v-model="levels" :config="config" :axis="axis" :options="options" @drill="drill" />
    </div>
    <div class="fs-chart__state" v-else><el-empty description="类型暂不支持，请在大屏中配置" /></div>
  </div>
</template>

<style lang="scss" scoped>
.fs-chart {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  &__body {
    flex: 1;
    min-height: 0;
  }
  &__state {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 15px;
    box-sizing: border-box;
    overflow: auto;
  }
}
</style>
