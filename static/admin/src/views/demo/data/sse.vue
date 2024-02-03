<template>
  <section>
    <a-card :bordered="false">
      <a-space slot="extra">
        <a-input v-model="url" />
        <a-button @click="toggle">{{ es ? 'Close' : 'Open' }}</a-button>
      </a-space>
      <p v-for="(message, i) in messages" :key="i">{{ message }}</p>
    </a-card>
  </section>
</template>

<script>
import base from '@/core/ServiceBase'

export default {
  data () {
    return {
      es: null,
      url: base.$axios.defaults.baseURL + 'proxy/getSSE?app=Demo&uri=/sse',
      messages: []
    }
  },
  methods: {
    toggle () {
      if (this.es) {
        this.es.close()
        this.es = null
      } else {
        this.messages = []
        this.es = new EventSource(this.url, { withCredentials: true })
        this.messages.push('EventSource.CONNECTING ' + this.es.readyState)
        this.es.onopen = () => {
          this.messages.push('EventSource.OPEN ' + this.es.readyState)
        }
        this.es.onmessage = (event) => {
          this.messages.push(event.data)
        }
        this.es.onerror = (event) => { // 若不主动关闭，浏览器会重新发起请求，无限循环
          this.es.close()
          this.es = null
          this.messages.push('EventSource.CLOSED ' + event.target.readyState)
        }
      }
    }
  },
  mounted () {
  }
}
</script>
