import { nextTick, ref } from 'vue'
import { defineStore } from 'pinia'
import ApiUtil from '@renderer/utils/ApiUtil'
import UserApi from '@renderer/api/UserApi'
import Db from '@renderer/core/Db'

interface UserInfo {
  id: number
  serial: string
  name: string
  email: string
  token: string
  avatar?: string
  description?: string
  phone?: string
  createdTime?: string
  loginTime?: string
}

type UserPayload = Partial<UserInfo> & { info?: Partial<UserInfo> }

export const useUserStore = defineStore('user', () => {
  const STORAGE_KEY = 'fs_auth_token'
  /** 还原登录态的等待上限：服务端不可达时也把界面放出来，别一直挡在门外 */
  const RESTORE_TIMEOUT = 10 * 1000

  const USER_DEFAULT_STATE = {
    id: 0,
    serial: '',
    name: '',
    email: '',
    token: ''
  }

  const ready = ref(false)
  const info = ref<UserInfo>(Object.assign({}, USER_DEFAULT_STATE))

  const reload = async (resize = false): Promise<void> => {
    Object.assign(info.value, { token: localStorage.getItem(STORAGE_KEY) || '' })
    if (info.value.token) {
      let result: unknown
      try {
        result = await UserApi.info()
      } catch {
        await reset(null, false)
        return
      }
      if (resize) {
        await nextTick()
        await window.ipc
          .setNormalSize()
          .catch((error) => console.error('[UserStore] Failed to resize window:', error))
      }
      await reset(ApiUtil.data(result), true)
    }
  }

  const reset = async (data: UserPayload | null = {}, isReady = true): Promise<void> => {
    // ready 的口径是「登录态与用户库都可用」：还原 / 初始化期间先降下来，
    // 依赖本地库的逻辑（App 的登录准备、login 页的跳转）只在它为 true 时动手
    ready.value = false
    Object.assign(info.value, USER_DEFAULT_STATE, data?.info ?? data ?? {})
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

  const restoreSession = async (resize = true): Promise<void> => {
    Object.assign(info.value, { token: localStorage.getItem(STORAGE_KEY) || '' })
    if (!info.value.token) {
      await reset()
      return
    }
    let timedOut = false
    await Promise.race([
      reload(resize),
      new Promise<void>((resolve) => {
        setTimeout(() => {
          timedOut = true
          resolve()
        }, RESTORE_TIMEOUT)
      })
    ])
    if (timedOut) console.warn('[UserStore] Session restore timed out')
  }

  /** 路由多次导航会重复调用：同一份登录态只还原一次 */
  let initializing: Promise<void> | null = null
  const initialize = (resize = true): Promise<void> => {
    if (!initializing) initializing = restoreSession(resize)
    return initializing
  }

  const isLogined = (): boolean => {
    return info.value.id > 0 && info.value.token !== ''
  }

  return { ready, info, reload, reset, initialize, isLogined, USER_DEFAULT_STATE }
})
