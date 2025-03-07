import './assets/main.scss'
import 'element-plus/dist/index.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import directive from './core/directive'

const app = createApp(App)

app.use(createPinia())
app.use(router)

for (const name in directive) {
  app.directive(name, (directive as any)[name])
}

app.mount('#app')
