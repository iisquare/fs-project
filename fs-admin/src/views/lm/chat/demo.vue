<script setup lang="ts">
import ProxyApi from '@/api/lm/ProxyApi';
import ApiUtil from '@/utils/ApiUtil';
import { onMounted, ref, watch } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import DataUtil from '@/utils/DataUtil';
import FetchEventSource from '@/core/FetchEventSource';
import { useUserStore } from '@/stores/user';

const form: any = ref({
  stream: true,
})
const loading = ref(false)
const agents: any = ref([])
const active = ref('output')
const rules = ref({
  agentId: [{ required: true, message: '请选择智能体', trigger: 'change' }]
})

watch(() => form.value.agentId, (id) => {
  const map = DataUtil.array2map(agents.value, 'id')
  const agent = Object.assign({
    systemPrompt: '',
    maxTokens: 0,
    temperature: 0,
    parameter: '',
  }, map[id] || {})
  Object.assign(form.value, {
    systemPrompt: agent.systemPrompt,
    maxTokens: agent.maxTokens,
    temperature: agent.temperature,
    parameter: agent.parameter,
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
const state = ref('')
const reason = ref('')
const thinking = ref(false)
const user = useUserStore()
const sse = new FetchEventSource('LM', '/chat/demo')
sse.addHeaders({ "X-Auth-Token": user.info.token })
sse.onOpen(() => {
  state.value = 'connect'
  reason.value = ''
  thinking.value = false
}).onMessage((message: any) => {
  if (!message.data) return
  const data = JSON.parse(message.data)
  state.value = data.action
  switch (data.action) {
    case 'choices.message':
      data.data.forEach((item: any) => {
        const msg = item.delta || item.message
        if (msg.reasoning_content) {
          if (!form.value.output) {
            thinking.value = true
            form.value.output += '<think>\n'
          }
          form.value.output += msg.reasoning_content
        }
        if (thinking.value && null !== msg.content) {
          thinking.value = false
          form.value.output += '</think>\n'
        }
        if (null !== msg.content) {
          form.value.output += msg.content
        }
        reason.value = item.finish_reason
      })
      break
    default:
      form.value.output += '\n' + message.data
      reason.value = data.action
  }
}).onError((error: any) => {
  state.value = 'error'
  form.value.output += '\n' + error
}).onClose(() => {
  state.value = 'finish'
  loading.value = false
})
const handleSubmit = () => {
  if (loading.value) {
    loading.value = false
    sse.abort()
  } else {
    loading.value = true
    form.value.output = ''
    sse.send(form.value)
  }
}

onMounted(() => {
  handleAgent()
})
</script>

<template>
  <el-container>
    <el-aside class="aside">
      <el-form :model="form" :rules="rules" label-position="top">
        <el-form-item label="智能体" prop="agentId">
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
          <el-input v-model="form.temperature" placeholder="为0时采用系统默认配置" />
        </el-form-item>
        <el-form-item label="自定义参数">
          <el-input type="textarea" v-model="form.parameter" :rows="5" placeholder="JSON格式" />
        </el-form-item>
      </el-form>
    </el-aside>
    <el-main>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户输入">
          <el-input type="textarea" v-model="form.input" :rows="8" placeholder="" />
        </el-form-item>
        <el-form-item label="" class="tools">
          <el-space>
            <el-button type="primary" @click="handleSubmit" :icon="loading ? ElementPlusIcons.Aim : ElementPlusIcons.Promotion">{{ loading ? '停止' : '发送' }}</el-button>
            <el-checkbox v-model="form.stream">流式输出</el-checkbox>
          </el-space>
          <el-space>
            <el-icon><ElementPlusIcons.InfoFilled /></el-icon>
            <span>{{ state }}</span>
            <span>{{ reason }}</span>
          </el-space>
        </el-form-item>
        <el-tabs v-model="active">
          <el-tab-pane label="模型输出" name="output"></el-tab-pane>
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
.tools {
  :deep(.el-form-item__content) {
    @include flex-between();
  }
}
</style>
