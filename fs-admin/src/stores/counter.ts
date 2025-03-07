import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useCounterStore = defineStore('counter', () => {
  const count = ref(0)
  const doubleCount = computed(() => count.value * 2)
  function increment() {
    count.value++
  }

  const routing = ref(false) // 路由正在跳转
  const fetching = ref(false) // 正在请求接口

  return {
    count,
    doubleCount,
    increment,
    routing,
    fetching,
  }
})
