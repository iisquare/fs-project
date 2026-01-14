<script setup lang="ts">
import ToolApi from '@/api/spider/ToolApi';
import CodeEditor from '@/components/Editor/CodeEditor.vue';
import { ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
const loading = ref(false)
const form: any = ref({
  expression: `Jsoup解析器使用方式：
jsoup {
    title: title(),
    -: body() {
        -1: select(".class-object") {
            detail: html(),
            property: select(".class-name") [
                key: select(".class-name").text(),
                value: select(".class-name").text()
            ]
        },
        -2: select(".class-array") [
            href: attr("abs:href"),
            description: select(".class-name:eq(0) > .class-name").html()
        ]
    }
}
`,
})
const parsed = ref('')
const handleSubmit = () => {
  loading.value = true
  ToolApi.parse(form.value).then(result => {
    parsed.value = JSON.stringify(result, null, 4)
  }).catch(result => {
    parsed.value = JSON.stringify(result, null, 4)
  }).finally(() => {
    loading.value = false
  })
}
</script>

<template>
  <el-container class="wh-full">
    <el-container>
      <el-aside width="500px">
        <el-card :bordered="false" shadow="never" header="表达式">
          <CodeEditor v-model="form.expression" :height="646" mode="javascript" />
        </el-card>
      </el-aside>
      <el-main>
        <el-form :model="form" label-position="top">
          <el-form-item label="页面地址">
            <el-input v-model="form.baseUri" placeholder="用于将页面中的相对链接地址转换为绝对地址" />
          </el-form-item>
          <el-form-item label="待解析数据">
            <el-input type="textarea" v-model="form.html" :rows="6" />
          </el-form-item>
          <el-form-item>
            <el-space>
              <el-button type="primary" @click="handleSubmit" :loading="loading" :icon="ElementPlusIcons.MagicStick">解析</el-button>
            </el-space>
          </el-form-item>
          <el-form-item label="解析结果">
            <CodeEditor v-model="parsed" :volatile="parsed" fold-gutter :height="350" mode="javascript" />
          </el-form-item>
        </el-form>
      </el-main>
    </el-container>
  </el-container>
</template>

<style lang="scss" scoped>
.el-aside {
  overflow: hidden;
  .el-card {
    margin-top: 5px;
    :deep(.el-card__body) {
      padding: 0;
    }
  }
}
</style>
