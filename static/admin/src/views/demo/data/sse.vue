<template>
  <section>
    <a-card :bordered="false">
      <a-space slot="extra">
        <a-input v-model="url" />
        <a-button @click="toggle">{{ es }}</a-button>
      </a-space>
      <p v-for="(message, i) in messages" :key="i">{{ message }}</p>
    </a-card>
  </section>
</template>

<script>
import base from '@/core/ServiceBase'
import { fetchEventSource } from '@microsoft/fetch-event-source'

export default {
  data () {
    return {
      es: 'OPEN',
      url: base.$axios.defaults.baseURL + 'proxy/postSSE',
      messages: []
    }
  },
  methods: {
    toggle () {
      const _this = this
      switch (_this.es) {
        case 'OPEN':
          _this.es = 'CLOSE'
          _this.messages = []
          const ctrl = new AbortController()
          fetchEventSource(this.url, {
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
              _this.messages.push('onopen ' + response.ok)
            },
            onmessage (msg) {
              _this.messages.push('onmessage ' + JSON.stringify(msg))
              if (_this.es === 'ABORT') {
                ctrl.abort('abort manually')
              }
            },
            onclose () {
              _this.messages.push('onclose')
            },
            onerror (err) {
              _this.messages.push('onerror ' + err)
              throw err // rethrow to stop the operation or return a specific retry interval
            }
          }).then(r => {
            _this.messages.push('fetch promise then ' + r)
          }).catch(e => {
            _this.messages.push('fetch promise catch ' + e)
          }).finally(() => {
            _this.messages.push('fetch promise finally')
            _this.es = 'OPEN'
          })
          break
        default:
          _this.es = 'ABORT'
      }
    }
  },
  mounted () {
  }
}
</script>
