<script setup lang="ts">
import ProxyApi from '@/api/lm/ProxyApi';
import ApiUtil from '@/utils/ApiUtil';
import { onMounted, ref, watch } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import DataUtil from '@/utils/DataUtil';

const form: any = ref({})
const loading = ref(false)
const agents: any = ref([])

watch(() => form.value.agentId, (id) => {
  const map = DataUtil.array2map(agents.value, 'id')
  const agent = Object.assign({
    systemPrompt: '',
    maxTokens: 0,
    temperature: 0.6,
  }, map[id] || {})
  Object.assign(form.value, {
    systemPrompt: agent.systemPrompt,
    maxTokens: agent.maxTokens,
    temperature: agent.temperature,
  })
}, { immediate: true })
const handleAgent = (success = false) => {
  loading.value = true
  ProxyApi.agents({ success }).then(result => {
    agents.value = ApiUtil.data(result)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
const handleSubmit = () => {
  loading.value = true
  form.value.output = ''
}

onMounted(() => {
  handleAgent()
})
</script>

<template>
  <el-container>
    <el-aside class="aside">
      <el-form :model="form" label-position="top">
        <el-form-item label="智能体">
          <el-space>
            <el-select v-model="form.agentId" placeholder="请选择" filterable clearable style="width: 200px;">
              <el-option v-for="agent in agents" :key="agent.id" :value="agent.id" :label="agent.name" />
            </el-select>
            <el-button :icon="ElementPlusIcons.Refresh" title="重新载入智能体" :loading="loading" @click="handleAgent(true)" />
          </el-space>
        </el-form-item>
        <el-form-item label="系统提示词">
          <el-input type="textarea" v-model="form.systemPrompt" :rows="5" placeholder="留空为不增加系统提示词" />
        </el-form-item>
        <el-form-item label="生成最大令牌数">
          <el-input v-model="form.maxTokens" placeholder="为0时采用系统默认配置" />
        </el-form-item>
        <el-form-item label="生成多样性">
          <el-input v-model="form.temperature" />
        </el-form-item>
      </el-form>
    </el-aside>
    <el-main>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户输入">
          <el-input type="textarea" v-model="form.input" :rows="8" placeholder="" />
        </el-form-item>
        <el-form-item label="">
          <el-space>
            <el-button type="primary" :loading="loading" @click="handleSubmit">发送</el-button>
            <el-checkbox v-model="form.stream">流式输出</el-checkbox>
          </el-space>
        </el-form-item>
        <el-tabs>
          <el-tab-pane label="模型输出1" name="first"></el-tab-pane>
        </el-tabs>
        <el-form-item label="">
          <el-input type="textarea" v-model="form.output" :rows="12" placeholder="" />
        </el-form-item>
      </el-form>
    </el-main>
  </el-container>
</template>

<style lang="scss" scoped>
.aside {
  width: 300px;
  height: calc(100vh - var(--fs-layout-header-height));
  overflow-x: hidden;
  overflow-y: auto;
  border-right: solid 1px var(--fs-layout-border-color);
  box-sizing: border-box;
  padding: 20px;
}
</style>
