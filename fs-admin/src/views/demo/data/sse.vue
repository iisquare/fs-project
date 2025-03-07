<script setup lang="ts">
import api from '@/core/Api';
import { ref } from 'vue';
import { fetchEventSource } from '@microsoft/fetch-event-source';

const es = ref('OPEN')
const url = ref(api.$axios.defaults.baseURL + '/proxy/postSSE')
const messages: any = ref([])

let ctrl: any = null
const toggle = () => {
  switch (es.value) {
    case 'OPEN':
      es.value = 'CLOSE'
      messages.value = []
      ctrl = new AbortController()
      fetchEventSource(url.value, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          app: 'Demo',
          uri: '/sse',
          data: {
            foo: 'bar'
          }
        }),
        signal: ctrl.signal,
        openWhenHidden: true,
        async onopen (response) {
          // 网络异常时不会触发，服务端异常时会触发但ok == false
          messages.value.push('onopen ' + response.ok)
        },
        onmessage (msg) {
          messages.value.push('onmessage ' + JSON.stringify(msg))
        },
        onclose () {
          // 只有正常执行完成才会触发，手动结束不会触发
          messages.value.push('onclose')
        },
        onerror (err) {
          messages.value.push('onerror ' + err)
          throw err // rethrow to stop the operation or return a specific retry interval
        }
      }).then(r => {
        // 在open后只要没有执行异常都会触发
        messages.value.push('fetch promise then ' + r)
      }).catch(e => {
        // 网络异常时会触发，服务端异常时不会触发，捕获到执行异常时会触发
        messages.value.push('fetch promise catch ' + e)
      }).finally(() => {
        // 始终会触发
        messages.value.push('fetch promise finally')
        es.value = 'OPEN'
      })
      break
    case 'CLOSE':
      es.value = 'ABORT'
      ctrl && ctrl.abort('abort manually') // 手动结束
      break
  }
}
</script>

<template>
  <el-card>
    <template #header>
      <el-space>
        <el-input v-model="url" style="width: 300px" />
        <el-button @click="toggle">{{ es }}</el-button>
      </el-space>
    </template>
    <p v-for="(message, i) in messages" :key="i">{{ message }}</p>
  </el-card>
</template>

<style lang="scss" scoped>
</style>
