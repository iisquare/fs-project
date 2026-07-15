<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import FileApi from '@/api/server/FileApi'
import FormUtil from '@/utils/FormUtil'
import { ElMessage } from 'element-plus'

const config: any = ref({ ready: false, status: {}, scale: {}, position: {} })
const generating = ref(false)
const result: any = ref({})

const form = ref({
  ids: '',
  width: 800,
  height: 800,
  clip: '',
  quality: 0.75,
  token: false
})

const scaleOptions = computed(() => {
  return Object.entries(config.value.scale).map(([id, item]: [string, any]) => ({
    value: id,
    label: item.label
  }))
})

const positionOptions = computed(() => {
  return Object.entries(config.value.position).map(([id, item]: [string, any]) => ({
    value: id,
    label: item.label
  }))
})

const previewSrcList = computed(() => imageList.value.filter(i => i.url).map(i => i.url))

const copiedIds = reactive<Set<string>>(new Set())
const dimensions = reactive<Record<string, { width: number; height: number }>>({})

const handleImageLoad = (id: string, e: Event) => {
  const img = e.target as HTMLImageElement
  dimensions[id] = { width: img.naturalWidth, height: img.naturalHeight }
}

const imageList = computed(() => {
  const entries = Object.entries(result.value)
  if (entries.length === 0) return []
  return entries.map(([id, item]: [string, any]) => {
    return {
      id,
      url: item.url,
      uri: item.uri,
      name: item.name,
      bucket: item.bucket,
      filepath: item.filepath,
      error: !item.url
    }
  })
})

onMounted(() => {
  FileApi.archiveConfig().then((res: any) => {
    config.value.ready = true
    if (res.code === 0) {
      Object.assign(config.value, res.data)
    }
  })
})

const handleGenerate = () => {
  if (generating.value) return
  const ids = form.value.ids
    .split(/[\n,]/)
    .map((s: string) => s.trim())
    .filter(Boolean)
  if (ids.length === 0) return

  const params: any = {}
  ids.forEach((id: string) => {
    params[id] = {
      type: 'image',
      width: form.value.width + '',
      height: form.value.height + ''
    }
    if (form.value.clip) {
      params[id].clip = form.value.clip
    }
    if (form.value.quality !== 0.75) {
      params[id].quality = form.value.quality + ''
    }
    if (form.value.token) {
      params[id].token = true
    }
  })

  generating.value = true
  FileApi.archiveUrl(params).then((res: any) => {
    if (res.code === 0) {
      result.value = res.data
    }
  }).catch(() => {}).finally(() => {
    generating.value = false
  })
}

const handleCopy = (item: any) => {
  FormUtil.copyToClipboard(item.url).then(() => {
    copiedIds.add(item.id)
    setTimeout(() => copiedIds.delete(item.id), 2000)
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
</script>

<template>
  <section>
    <el-row :gutter="15">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>图片参数</template>
          <el-form :model="form" label-width="80px">
            <el-form-item label="文件标识">
              <el-input
                v-model="form.ids"
                type="textarea"
                :rows="5"
                placeholder="文件ID，支持多个（换行或英文逗号分隔）"
              />
            </el-form-item>

            <el-form-item label="宽度">
              <el-input-number v-model="form.width" :min="16" :max="4096" />
            </el-form-item>

            <el-form-item label="高度">
              <el-input-number v-model="form.height" :min="16" :max="4096" />
            </el-form-item>

            <el-form-item label="缩放模式">
              <el-select v-model="form.clip" placeholder="默认不缩放" clearable>
                <el-option
                  v-for="opt in scaleOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
                <el-option
                  v-for="opt in positionOptions"
                  :key="opt.value"
                  :label="'剪裁' + opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="图片质量">
              <el-slider
                v-model="form.quality"
                :min="0"
                :max="1"
                :step="0.05"
                :format-tooltip="(val: number) => Math.round(val * 100) + '%'"
              />
            </el-form-item>

            <el-form-item label="无水印图">
              <el-switch v-model="form.token" active-text="是" inactive-text="否" />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="generating"
                @click="handleGenerate"
              >
                生成地址
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never">
          <template #header>预览结果</template>
          <div v-if="!generating && imageList.length === 0" style="color: #909399; text-align: center; padding: 60px 0;">
            请输入文件标识并点击"生成地址"
          </div>

          <div v-if="imageList.length > 0" class="preview-list">
            <el-card
              v-for="item in imageList"
              :key="item.id"
              shadow="hover"
              class="preview-item"
            >
              <template #header>
                <div class="preview-item-header flex-between">
                  <div class="preview-item-left">
                    <span class="preview-item-id">{{ item.id }}</span>
                    <span v-if="dimensions[item.id]" class="preview-dimensions">
                      {{ dimensions[item.id].width }} × {{ dimensions[item.id].height }}
                    </span>
                  </div>
                  <el-tag v-if="item.name" size="small">{{ item.name }}</el-tag>
                </div>
              </template>
              <div class="preview-item-body">
                <div class="preview-image-wrap">
                  <el-image
                    v-if="item.url"
                    :src="item.url"
                    fit="contain"
                    :preview-src-list="previewSrcList"
                    :initial-index="previewSrcList.indexOf(item.url)"
                    :preview-teleported="true"
                    style="max-width: 100%;"
                    @load="(e: Event) => handleImageLoad(item.id, e)"
                  />
                  <div v-else class="preview-placeholder">文件不可用</div>
                </div>
                <div class="preview-url">
                  <div class="preview-url-label">访问地址</div>
                  <el-input
                    :model-value="item.url || '—'"
                    readonly
                    size="small"
                    class="preview-url-input"
                  >
                    <template #append>
                      <el-button
                        v-if="item.url"
                        :type="copiedIds.has(item.id) ? 'success' : 'default'"
                        @click="handleCopy(item)"
                      >
                        {{ copiedIds.has(item.id) ? '已复制' : '复制' }}
                      </el-button>
                    </template>
                  </el-input>
                </div>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style lang="scss" scoped>
.preview-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.preview-item-header {
  gap: 10px;
}

.preview-item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-item-id {
  font-family: monospace;
  font-size: 13px;
}

.preview-dimensions {
  font-size: 12px;
  color: #909399;
}

.preview-item-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-image-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 120px;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 10px;
}

.preview-placeholder {
  color: #c0c4cc;
  font-size: 14px;
}

.preview-url-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.preview-url-input {
  :deep(.el-input__inner) {
    font-size: 12px;
    font-family: monospace;
  }
}
</style>
