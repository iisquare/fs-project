<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import ProxyApi from '@/api/lm/ProxyApi';
import ApiUtil from '@/utils/ApiUtil';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/user';
import FetchEventSource from '@/core/FetchEventSource';
import type { FormInstance } from 'element-plus';

const form: any = ref({
  stream: true,
  column: 1,
  agentIds: [],
})
const formRef: any = ref<FormInstance>()
const agents: any = ref([])
const rules = ref({
  agentIds: [{ required: true, message: '请选择智能体', trigger: 'change' }]
})
const loading = computed(() => {
  for (const agent of agents.value) {
    if (agent.loading) return true
  }
  return false
})
const user = useUserStore()
const handleAgent = (success = false) => {
  ProxyApi.agents({ success }).then(result => {
    agents.value = ApiUtil.data(result) // 直接在此处map，元素是普通对象，修改时无法触发数据监听
    agents.value.map((agent: any) => { // 先赋值之后，此处拿到的监听对象，修改值后可以直接触发数据更新
      agent.state = ''
      agent.reason = ''
      agent.thinking = false
      agent.loading = false
      const sse = new FetchEventSource('LM', '/chat/compare')
      sse.addHeaders({ "X-Auth-Token": user.info.token })
      sse.onOpen(() => {
        agent.state = 'connect'
        agent.reason = ''
        agent.thinking = false
        agent.loading = true
        agent.output = ''
      }).onMessage((message: any) => {
        if (!message.data) return
        const data = JSON.parse(message.data)
        agent.state = data.action
        switch (data.action) {
          case 'choices.message':
            data.data.forEach((item: any) => {
              const msg = item.delta || item.message
              if (msg.reasoning_content) {
                if (!agent.output) {
                  agent.thinking = true
                  agent.output += '<think>\n'
                }
                agent.output += msg.reasoning_content
              }
              if (agent.thinking && null !== msg.content) {
                agent.thinking = false
                agent.output += '</think>\n'
              }
              if (null !== msg.content) {
                agent.output += msg.content
              }
              agent.reason = item.finish_reason
            })
            break
          default:
            agent.output += '\n' + message.data
            agent.reason = data.action
        }
      }).onError((error: any) => {
        agent.state = 'error'
        agent.output += '\n' + error
      }).onClose(() => {
        agent.state = 'finish'
        agent.loading = false
      })
      agent.sse = sse
      return agent
    })
  }).catch(() => {}).finally(() => {})
}

const handleSubmit = () => {
  if (loading.value) {
    for (const agent of agents.value) {
      if (form.value.agentIds.indexOf(agent.id) === -1) continue
      agent.sse.abort()
    }
    return
  }
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    for (const agent of agents.value) {
      if (form.value.agentIds.indexOf(agent.id) === -1) continue
      agent.sse.send({
        agentId: agent.id,
        input: form.value.input,
        stream: form.value.stream,
      })
    }
  })
}

const marks = computed(() => {
  const result: any = {}
  for (let i = 1; i <= 8; i++) {
    if (24 % i === 0) {
      result[i] = `${i}`
    }
  }
  return result
})

onMounted(() => {
    handleAgent()
})
</script>

<template>
  <el-card :bordered="false" shadow="never">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-form-item label="选择模型" prop="agentIds">
        <el-select v-model="form.agentIds" placeholder="请选择" filterable clearable multiple :disabled="loading">
          <el-option v-for="agent in agents" :key="agent.id" :value="agent.id" :label="agent.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户输入">
        <el-input type="textarea" v-model="form.input" :rows="8" placeholder="" />
      </el-form-item>
      
      <el-form-item label="执行操作">
        <el-space>
          <el-button type="primary" @click="handleSubmit" :icon="loading ? ElementPlusIcons.Aim : ElementPlusIcons.Promotion">{{ loading ? '停止' : '发送' }}</el-button>
          <el-checkbox v-model="form.stream">流式输出</el-checkbox>
        </el-space>
      </el-form-item>
      <el-form-item label="每行列数">
        <el-slider v-model="form.column" :min="1" :max="8" :step="1" :marks="marks" />
      </el-form-item>
    </el-form>
    <el-row :gutter="15" class="result">
      <template v-for="agent in agents" :key="agent.id">
        <el-col :span="Math.ceil(24 / form.column)" v-if="form.agentIds.indexOf(agent.id) !== -1">
          <el-card :bordered="false" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>{{ agent.name }}</span>
                <el-space>
                  <el-icon><ElementPlusIcons.InfoFilled /></el-icon>
                  <span>{{ agent.state }}</span>
                  <span>{{ agent.reason }}</span>
                </el-space>
              </div>
            </template>
            <el-input type="textarea" v-model="agent.output" :rows="12" placeholder="" />
          </el-card>
        </el-col>
      </template>
    </el-row>
  </el-card>
</template>

<style lang="scss" scoped>
.el-slider {
  margin-bottom: 25px;
}
:deep(.card-header) {
  @include flex-between();
}
.result {
  .el-card {
    margin-bottom: 15px;
  }
}
</style>
