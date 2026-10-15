import { ref } from 'vue'
import { defineStore } from 'pinia'
import ApiUtil from '@renderer/utils/ApiUtil'
import UserApi from '@renderer/api/UserApi'
import Db from '@renderer/core/Db'

export const useUserStore = defineStore('user', () => {
  const STORAGE_KEY = 'fs_auth_token'

  const USER_DEFAULT_STATE = {
    id: 0,
    serial: '',
    name: '',
    email: '',
    token: ''
  }

  const ready = ref(false)
  const info: any = ref(Object.assign({}, USER_DEFAULT_STATE))

  const reload = async (resize = false) => {
    Object.assign(info.value, { token: localStorage.getItem(STORAGE_KEY) || '' })
    if (info.value.token) {
      try {
        const result: any = await UserApi.info()
        if (resize) {
          nextTick(() => {
            window.ipc.setNormalSize()
          })
        }
        await reset(ApiUtil.data(result), true)
      } catch {
        await reset(null, false)
      }
    }
  }

  const reset = async (data: any = {}, isReady = true) => {
    // ready 的口径是「登录态与用户库都可用」：还原 / 初始化期间先降下来，
    // 依赖本地库的逻辑（App 的登录准备、login 页的跳转）只在它为 true 时动手
    ready.value = false
    Object.assign(info.value, USER_DEFAULT_STATE, data?.info ?? data)
    if (info.value.token) {
      localStorage.setItem(STORAGE_KEY, info.value.token)
      if (info.value.id > 0 && info.value.serial) {
        try {
          await Db.initUser(
            info.value.serial,
            String(info.value.id),
            import.meta.env.VITE_DB_SECRET
          )
        } catch (e) {
          console.error('[UserStore] Failed to initialize user database:', e)
          isReady = false
        }
      }
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
    ready.value = isReady
  }

  const initialize = async () => {
    Object.assign(info.value, { token: localStorage.getItem(STORAGE_KEY) || '' })
    if (info.value.token) {
      await reload(true)
    } else {
      reset()
    }
  }

  const isLogined = () => {
    return info.value.id > 0 && info.value.token !== ''
  }

  return { ready, info, reload, reset, initialize, isLogined, USER_DEFAULT_STATE }
})
