<script setup lang="ts">
import { ref } from 'vue'
import { genFileId } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import type { UploadInstance, UploadRawFile } from 'element-plus'

const props = withDefaults(defineProps<{
  limit?: number
  uploading?: boolean
  progress?: number
  loaded?: number
  total?: number
  processingText?: string
}>(), {
  limit: 1,
  uploading: false,
  progress: 0,
  loaded: 0,
  total: 0,
  processingText: '文件上传完成，正在处理...'
})

const model = defineModel<File | File[]>()

const uploadRef = ref<UploadInstance>()
const fileList = ref<any[]>([])

const handleChange = (f: any) => {
  if (props.limit === 1) {
    fileList.value = [f]
    model.value = f.raw
  } else {
    fileList.value.push(f)
    model.value = fileList.value.map((item: any) => item.raw)
  }
}

const handleExceed = (files: File[]) => {
  if (props.limit !== 1) return
  uploadRef.value!.clearFiles()
  const file = files[0] as UploadRawFile
  file.uid = genFileId()
  uploadRef.value!.handleStart(file)
}

const handleRemove = (_file: any, list: any[]) => {
  fileList.value = list
  model.value = props.limit === 1 ? undefined : list.map((item: any) => item.raw)
}

const formatSize = (bytes: number): string => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}
</script>

<template>
  <div class="form-upload">
    <el-upload
      ref="uploadRef"
      :file-list="fileList"
      :auto-upload="false"
      :limit="limit"
      :disabled="uploading"
      drag
      @change="handleChange"
      @exceed="handleExceed"
      @remove="handleRemove"
    >
      <el-icon size="24"><ElementPlusIcons.UploadFilled /></el-icon>
      <div class="el-upload__text">拖拽文件到此处 或 <em>点击上传</em></div>
    </el-upload>
    <div v-if="uploading" class="form-upload__progress">
      <el-progress :percentage="Math.min(100, Math.max(0, progress))" :stroke-width="8" />
      <span class="form-upload__progress-text">
        <template v-if="progress < 100">
          已上传 {{ formatSize(loaded) }} / {{ formatSize(total) }}
        </template>
        <template v-else>{{ processingText }}</template>
      </span>
    </div>
  </div>
  
</template>

<style lang="scss" scoped>
.form-upload {
  width: 100%;
}

.form-upload__progress {
  width: 100%;
  margin-top: 14px;
}

.form-upload__progress-text {
  display: block;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
