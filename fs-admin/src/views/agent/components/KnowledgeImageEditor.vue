<script setup lang="ts">
/**
 * 知识库 Markdown 编辑器
 * 在通用编辑器基础上接入知识库图片能力：图片标识的实时地址签发与图片上传
 * 知识库与所属文档为必填，用于权限判定与图片来源记录
 */
import MarkdownEditor from '@/components/Editor/MarkdownEditor.vue'
import KnowledgeImageApi from '@/api/agent/KnowledgeImageApi'
import ApiUtil from '@/utils/ApiUtil'

defineOptions({ inheritAttrs: false })

const model: any = defineModel()
const props = defineProps({
  knowledgeId: { type: [String, Number], required: true },
  documentId: { type: [String, Number], required: true },
})

const resolve = async (ids: string[]) => {
  const result: any = await KnowledgeImageApi.url({ knowledgeId: props.knowledgeId, ids })
  return ApiUtil.data(result) || {}
}

const upload = async (file: File) => {
  const result: any = await KnowledgeImageApi.upload({
    file,
    knowledgeId: props.knowledgeId,
    documentId: props.documentId,
    alt: file.name,
  }, {})
  const data = ApiUtil.data(result) || {}
  return data.id ? data : null
}
</script>

<template>
  <MarkdownEditor v-model="model" v-bind="$attrs" :resolve-images="resolve" :upload-image="upload" />
</template>
