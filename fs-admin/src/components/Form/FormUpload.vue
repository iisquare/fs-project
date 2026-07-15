<script setup lang="ts">
import { ref } from 'vue'
import { genFileId } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import type { UploadInstance, UploadRawFile } from 'element-plus'

const props = withDefaults(defineProps<{
  limit?: number
}>(), {
  limit: 1
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
</script>

<template>
  <div class="form-upload">
    <el-upload
      ref="uploadRef"
      :file-list="fileList"
      :auto-upload="false"
      :limit="limit"
      drag
      @change="handleChange"
      @exceed="handleExceed"
      @remove="handleRemove"
    >
      <el-icon size="24"><ElementPlusIcons.UploadFilled /></el-icon>
      <div class="el-upload__text">拖拽文件到此处 或 <em>点击上传</em></div>
    </el-upload>
  </div>
  
</template>

<style lang="scss" scoped>
.form-upload {
  width: 100%;
}
</style>
